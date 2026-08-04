package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.exception.UserErrorException;
import com.edu.mapper.LearningPracticeMapper;
import com.edu.mapper.LearningQuestionMapper;
import com.edu.mapper.LearningSubmissionMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.practice.PracticeAiDraftRequest;
import com.edu.pojo.dto.practice.PracticeAnswerRequest;
import com.edu.pojo.dto.practice.PracticeReviewRequest;
import com.edu.pojo.dto.practice.PracticePublishRequest;
import com.edu.pojo.dto.practice.PracticeSubmitRequest;
import com.edu.pojo.po.LearningPracticePO;
import com.edu.pojo.po.LearningQuestionPO;
import com.edu.pojo.po.LearningSubmissionPO;
import com.edu.pojo.vo.practice.PracticeListItemVO;
import com.edu.pojo.vo.practice.StudentPracticeDetailVO;
import com.edu.pojo.vo.practice.TeacherPracticeSubmissionVO;
import com.edu.pojo.vo.practice.TeacherPracticeCourseVO;
import com.edu.service.LearningPracticeService;
import com.edu.util.SecurityUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningPracticeServiceImpl implements LearningPracticeService {
    private final LearningPracticeMapper practiceMapper;
    private final LearningQuestionMapper questionMapper;
    private final LearningSubmissionMapper submissionMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<PracticeListItemVO> listStudentPractices() {
        UserInfoDTO user = requireRole("STUDENT");
        return practiceMapper.selectList(new LambdaQueryWrapper<LearningPracticePO>()
                        .eq(LearningPracticePO::getStatus, 1)
                        .orderByAsc(LearningPracticePO::getCourseId))
                .stream()
                .map(practice -> toListItem(practice, findSubmission(practice.getId(), user.getUserId())))
                .toList();
    }

    @Override
    public StudentPracticeDetailVO getStudentPractice(Long practiceId) {
        UserInfoDTO user = requireRole("STUDENT");
        LearningPracticePO practice = requirePractice(practiceId);
        LearningSubmissionPO submission = findSubmission(practiceId, user.getUserId());
        return toStudentDetail(practice, submission);
    }

    @Override
    @Transactional
    public StudentPracticeDetailVO submitPractice(Long practiceId, PracticeSubmitRequest request) {
        UserInfoDTO user = requireRole("STUDENT");
        LearningPracticePO practice = requirePractice(practiceId);
        List<LearningQuestionPO> questions = questionsOf(practiceId);
        Map<Long, String> answers = request.getAnswers().stream()
                .collect(Collectors.toMap(PracticeAnswerRequest::getQuestionId,
                        item -> item.getAnswer().trim(), (first, ignored) -> first, LinkedHashMap::new));
        Set<Long> questionIds = questions.stream().map(LearningQuestionPO::getId).collect(Collectors.toSet());
        if (!answers.keySet().equals(questionIds)) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "请完成当前练习的全部题目后再提交");
        }
        questions.stream()
                .filter(question -> "SINGLE".equals(question.getQuestionType()))
                .forEach(question -> {
                    String answer = normalizeSingleChoice(answers.get(question.getId()));
                    int optionCount = readOptions(question.getOptionsJson()).size();
                    if (!StringUtils.hasText(answer) || answer.charAt(0) - 'A' >= Math.min(optionCount, 4)) {
                        throw new UserErrorException(HttpStatus.BAD_REQUEST,
                                "单选题答案只能是已有的 A、B、C、D 选项");
                    }
                    answers.put(question.getId(), answer);
                });

        LearningSubmissionPO submission = findSubmission(practiceId, user.getUserId());
        LocalDateTime now = LocalDateTime.now();
        if (submission == null) {
            submission = new LearningSubmissionPO();
            submission.setPracticeId(practiceId);
            submission.setStudentId(user.getUserId());
            submission.setStudentName(StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername());
            submission.setSubmitTime(now);
        }
        submission.setAnswerJson(writeAnswers(answers));
        submission.setQuestionReviewJson(null);
        submission.setAutoScore(calculateAutoScore(questions, answers));
        submission.setTeacherScore(null);
        submission.setTeacherFeedback(null);
        submission.setStatus("SUBMITTED");
        submission.setReviewTime(null);
        submission.setReviewerId(null);
        submission.setUpdateTime(now);
        if (submission.getId() == null) {
            submissionMapper.insert(submission);
        } else {
            submissionMapper.updateById(submission);
        }
        return toStudentDetail(practice, submission);
    }

    @Override
    public List<TeacherPracticeSubmissionVO> listTeacherSubmissions(String status) {
        UserInfoDTO user = requireRole("TEACHER");
        Map<Long, LearningPracticePO> practices = teacherPractices(user.getUserId());
        if (practices.isEmpty()) {
            return List.of();
        }
        return submissionMapper.selectList(new LambdaQueryWrapper<LearningSubmissionPO>()
                        .in(LearningSubmissionPO::getPracticeId, practices.keySet())
                        .eq(StringUtils.hasText(status), LearningSubmissionPO::getStatus, status)
                        .orderByDesc(LearningSubmissionPO::getSubmitTime))
                .stream()
                .map(submission -> toTeacherSubmission(submission, practices.get(submission.getPracticeId())))
                .toList();
    }

    @Override
    public TeacherPracticeSubmissionVO getTeacherSubmission(Long submissionId) {
        UserInfoDTO user = requireRole("TEACHER");
        LearningSubmissionPO submission = requireTeacherSubmission(submissionId, user);
        LearningPracticePO practice = requirePractice(submission.getPracticeId());
        return toTeacherSubmission(submission, practice);
    }

    @Override
    @Transactional
    public TeacherPracticeSubmissionVO saveAiReviewDraft(Long submissionId, PracticeAiDraftRequest request) {
        UserInfoDTO user = requireRole("TEACHER");
        LearningSubmissionPO submission = requireTeacherSubmission(submissionId, user);
        if (!"SUBMITTED".equals(submission.getStatus())) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "已完成批改的练习不能再保存 AI 草稿");
        }
        LearningPracticePO practice = requirePractice(submission.getPracticeId());
        LearningQuestionPO question = questionMapper.selectById(request.getQuestionId());
        if (question == null || !Objects.equals(question.getPracticeId(), practice.getId())
                || !"SHORT".equals(question.getQuestionType())) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "只能为当前练习的开放题保存 AI 草稿");
        }
        int maxScore = question.getQuestionScore() == null ? 0 : question.getQuestionScore();
        if (request.getScore() > maxScore) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "AI 建议分数不能超过本题满分 " + maxScore + " 分");
        }

        Map<Long, QuestionReviewDetail> reviews = new LinkedHashMap<>(
                readQuestionReviews(submission.getQuestionReviewJson()));
        reviews.put(request.getQuestionId(), new QuestionReviewDetail(
                request.getScore(),
                request.getFeedback().trim(),
                "AI_DRAFT",
                StringUtils.hasText(request.getReasoning()) ? request.getReasoning().trim() : null,
                request.getConfidence()
        ));
        submission.setQuestionReviewJson(writeQuestionReviews(reviews));
        submission.setUpdateTime(LocalDateTime.now());
        submissionMapper.updateById(submission);
        return toTeacherSubmission(submission, practice);
    }

    @Override
    @Transactional
    public TeacherPracticeSubmissionVO reviewSubmission(Long submissionId, PracticeReviewRequest request) {
        UserInfoDTO user = requireRole("TEACHER");
        LearningSubmissionPO submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "学生提交不存在");
        }
        LearningPracticePO practice = requirePractice(submission.getPracticeId());
        requireTeacherCourse(practice.getCourseId(), user.getUserId());
        List<LearningQuestionPO> questions = questionsOf(practice.getId());
        List<LearningQuestionPO> openQuestions = questions.stream()
                .filter(question -> "SHORT".equals(question.getQuestionType()))
                .toList();
        Map<Long, PracticeReviewRequest.QuestionReview> requestedReviews = new LinkedHashMap<>();
        for (PracticeReviewRequest.QuestionReview item : request.getQuestionReviews()) {
            if (requestedReviews.putIfAbsent(item.getQuestionId(), item) != null) {
                throw new UserErrorException(HttpStatus.BAD_REQUEST, "同一道开放题不能重复评分");
            }
        }
        Set<Long> openQuestionIds = openQuestions.stream().map(LearningQuestionPO::getId).collect(Collectors.toSet());
        if (!requestedReviews.keySet().equals(openQuestionIds)) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "请完成每道开放题的评分和反馈");
        }

        Map<Long, QuestionReviewDetail> existingReviews = readQuestionReviews(submission.getQuestionReviewJson());
        Map<Long, QuestionReviewDetail> questionReviews = new LinkedHashMap<>();
        int manualScore = 0;
        for (LearningQuestionPO question : openQuestions) {
            PracticeReviewRequest.QuestionReview item = requestedReviews.get(question.getId());
            int maxScore = question.getQuestionScore() == null ? 0 : question.getQuestionScore();
            if (item.getScore() > maxScore) {
                throw new UserErrorException(HttpStatus.BAD_REQUEST,
                        "开放题得分不能超过该题满分 " + maxScore + " 分");
            }
            manualScore += item.getScore();
            QuestionReviewDetail previous = existingReviews.get(question.getId());
            String source = previous != null && StringUtils.hasText(previous.source())
                    && previous.source().startsWith("AI")
                    ? "AI_CONFIRMED"
                    : "TEACHER";
            questionReviews.put(question.getId(), new QuestionReviewDetail(
                    item.getScore(),
                    item.getFeedback().trim(),
                    source,
                    previous == null ? null : previous.reasoning(),
                    previous == null ? null : previous.confidence()
            ));
        }
        int totalScore = (submission.getAutoScore() == null ? 0 : submission.getAutoScore()) + manualScore;
        if (!Objects.equals(request.getScore(), totalScore)) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "总分与各题得分不一致，请刷新后重新批改");
        }

        submission.setQuestionReviewJson(writeQuestionReviews(questionReviews));
        submission.setTeacherScore(totalScore);
        submission.setTeacherFeedback(request.getFeedback().trim());
        submission.setStatus("REVIEWED");
        submission.setReviewerId(user.getUserId());
        submission.setReviewTime(LocalDateTime.now());
        submission.setUpdateTime(LocalDateTime.now());
        submissionMapper.updateById(submission);
        return toTeacherSubmission(submission, practice);
    }

    @Override
    public List<TeacherPracticeCourseVO> listTeacherPracticeCourses() {
        UserInfoDTO user = requireRole("TEACHER");
        return jdbcTemplate.query(
                "SELECT id, course_name FROM edu_course WHERE teacher_id = ? ORDER BY id",
                (resultSet, rowNum) -> new TeacherPracticeCourseVO(
                        resultSet.getLong("id"), resultSet.getString("course_name")),
                user.getUserId()
        );
    }

    @Override
    @Transactional
    public Long publishPractice(PracticePublishRequest request) {
        UserInfoDTO user = requireRole("TEACHER");
        requireTeacherCourse(request.getCourseId(), user.getUserId());
        int scoreSum = request.getQuestions().stream().mapToInt(PracticePublishRequest.Question::getScore).sum();
        if (!Objects.equals(scoreSum, request.getTotalScore())) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "题目分值之和必须等于练习总分");
        }

        LearningPracticePO practice = new LearningPracticePO();
        LocalDateTime now = LocalDateTime.now();
        practice.setCourseId(request.getCourseId());
        practice.setPracticeTitle(request.getTitle().trim());
        practice.setPracticeIntro(StringUtils.hasText(request.getIntro()) ? request.getIntro().trim() : null);
        practice.setTotalScore(request.getTotalScore());
        practice.setStatus(1);
        practice.setCreateTime(now);
        practice.setUpdateTime(now);
        practiceMapper.insert(practice);

        for (int index = 0; index < request.getQuestions().size(); index++) {
            PracticePublishRequest.Question item = request.getQuestions().get(index);
            String type = item.getType().trim().toUpperCase();
            if (!Set.of("SINGLE", "SHORT").contains(type)) {
                throw new UserErrorException(HttpStatus.BAD_REQUEST, "题目类型只能是单选题或开放题");
            }
            List<String> options = item.getOptions() == null ? List.of() : item.getOptions().stream()
                    .map(value -> value == null ? "" : value.trim()).toList();
            if ("SINGLE".equals(type)) {
                if (options.size() != 4 || options.stream().anyMatch(String::isBlank)) {
                    throw new UserErrorException(HttpStatus.BAD_REQUEST, "单选题必须设置完整的 A、B、C、D 四个选项");
                }
                String answer = item.getReferenceAnswer().trim().toUpperCase();
                if (!answer.matches("[A-D]")) {
                    throw new UserErrorException(HttpStatus.BAD_REQUEST, "单选题正确答案只能是 A、B、C 或 D");
                }
            }
            LearningQuestionPO question = new LearningQuestionPO();
            question.setPracticeId(practice.getId());
            question.setQuestionType(type);
            question.setQuestionContent(item.getContent().trim());
            question.setOptionsJson("SINGLE".equals(type) ? writeOptions(options) : null);
            question.setReferenceAnswer(item.getReferenceAnswer().trim());
            question.setAnswerExplanation(StringUtils.hasText(item.getExplanation()) ? item.getExplanation().trim() : null);
            question.setQuestionScore(item.getScore());
            question.setSortOrder(index + 1);
            questionMapper.insert(question);
        }
        return practice.getId();
    }

    @Override
    @Transactional
    public void deletePractice(Long practiceId) {
        UserInfoDTO user = requireRole("TEACHER");
        LearningPracticePO practice = requirePractice(practiceId);
        requireTeacherCourse(practice.getCourseId(), user.getUserId());
        submissionMapper.delete(new LambdaQueryWrapper<LearningSubmissionPO>()
                .eq(LearningSubmissionPO::getPracticeId, practiceId));
        questionMapper.delete(new LambdaQueryWrapper<LearningQuestionPO>()
                .eq(LearningQuestionPO::getPracticeId, practiceId));
        practiceMapper.deleteById(practiceId);
    }

    private PracticeListItemVO toListItem(LearningPracticePO practice, LearningSubmissionPO submission) {
        CourseBrief course = findCourse(practice.getCourseId());
        return PracticeListItemVO.builder()
                .id(practice.getId())
                .courseId(practice.getCourseId())
                .courseName(course == null ? "课程练习" : course.title())
                .title(practice.getPracticeTitle())
                .intro(practice.getPracticeIntro())
                .totalScore(practice.getTotalScore())
                .questionCount(questionsOf(practice.getId()).size())
                .status(submission == null ? "NOT_STARTED" : submission.getStatus())
                .score(submission == null ? null : displayedScore(submission))
                .submitTime(submission == null ? null : submission.getSubmitTime())
                .build();
    }

    private StudentPracticeDetailVO toStudentDetail(LearningPracticePO practice, LearningSubmissionPO submission) {
        Map<Long, String> answers = submission == null ? Map.of() : readAnswers(submission.getAnswerJson());
        boolean canSeeReference = submission != null && "REVIEWED".equals(submission.getStatus());
        List<LearningQuestionPO> questions = questionsOf(practice.getId());
        Map<Long, QuestionReviewDetail> questionReviews = reviewDetailsFor(submission, questions);
        CourseBrief course = findCourse(practice.getCourseId());
        return StudentPracticeDetailVO.builder()
                .id(practice.getId())
                .courseId(practice.getCourseId())
                .courseName(course == null ? "课程练习" : course.title())
                .title(practice.getPracticeTitle())
                .intro(practice.getPracticeIntro())
                .totalScore(practice.getTotalScore())
                .submissionStatus(submission == null ? "NOT_STARTED" : submission.getStatus())
                .score(submission == null ? null : displayedScore(submission))
                .teacherFeedback(submission == null ? null : submission.getTeacherFeedback())
                .submitTime(submission == null ? null : submission.getSubmitTime())
                .questions(questions.stream()
                        .map(question -> StudentPracticeDetailVO.Question.builder()
                                .id(question.getId())
                                .type(question.getQuestionType())
                                .content(question.getQuestionContent())
                                .options(readOptions(question.getOptionsJson()))
                                .score(question.getQuestionScore())
                                .answer(displayAnswer(question, answers.get(question.getId())))
                                .referenceAnswer(canSeeReference ? question.getReferenceAnswer() : null)
                                .explanation(canSeeReference ? question.getAnswerExplanation() : null)
                                .awardedScore(canSeeReference
                                        ? awardedScore(question, answers.get(question.getId()), questionReviews)
                                        : null)
                                .teacherFeedback(canSeeReference && questionReviews.containsKey(question.getId())
                                        ? questionReviews.get(question.getId()).feedback()
                                        : null)
                                .build())
                        .toList())
                .build();
    }

    private TeacherPracticeSubmissionVO toTeacherSubmission(LearningSubmissionPO submission, LearningPracticePO practice) {
        CourseBrief course = findCourse(practice.getCourseId());
        Map<Long, String> answers = readAnswers(submission.getAnswerJson());
        List<LearningQuestionPO> questions = questionsOf(practice.getId());
        Map<Long, QuestionReviewDetail> questionReviews = reviewDetailsFor(submission, questions);
        return TeacherPracticeSubmissionVO.builder()
                .submissionId(submission.getId())
                .practiceId(practice.getId())
                .practiceTitle(practice.getPracticeTitle())
                .courseId(practice.getCourseId())
                .courseName(course == null ? "课程练习" : course.title())
                .totalScore(practice.getTotalScore())
                .studentId(submission.getStudentId())
                .studentName(submission.getStudentName())
                .autoScore(submission.getAutoScore())
                .teacherScore(submission.getTeacherScore())
                .feedback(submission.getTeacherFeedback())
                .status(submission.getStatus())
                .submitTime(submission.getSubmitTime())
                .reviewTime(submission.getReviewTime())
                .answers(questions.stream()
                        .map(question -> TeacherPracticeSubmissionVO.Answer.builder()
                                .questionId(question.getId())
                                .questionType(question.getQuestionType())
                                .questionContent(question.getQuestionContent())
                                .studentAnswer(displayAnswer(question, answers.get(question.getId())))
                                .referenceAnswer(question.getReferenceAnswer())
                                .explanation(question.getAnswerExplanation())
                                .score(question.getQuestionScore())
                                .awardedScore(awardedScore(question, answers.get(question.getId()), questionReviews))
                                .teacherFeedback(questionReviews.containsKey(question.getId())
                                        ? questionReviews.get(question.getId()).feedback()
                                        : null)
                                .reviewSource(questionReviews.containsKey(question.getId())
                                        ? questionReviews.get(question.getId()).source()
                                        : null)
                                .aiReasoning(questionReviews.containsKey(question.getId())
                                        ? questionReviews.get(question.getId()).reasoning()
                                        : null)
                                .aiConfidence(questionReviews.containsKey(question.getId())
                                        ? questionReviews.get(question.getId()).confidence()
                                        : null)
                                .build())
                        .toList())
                .build();
    }

    private Map<Long, LearningPracticePO> teacherPractices(Long teacherId) {
        return practiceMapper.selectList(new LambdaQueryWrapper<LearningPracticePO>().eq(LearningPracticePO::getStatus, 1))
                .stream()
                .filter(practice -> {
                    CourseBrief course = findCourse(practice.getCourseId());
                    return course != null && Objects.equals(course.teacherId(), teacherId);
                })
                .collect(Collectors.toMap(LearningPracticePO::getId, item -> item));
    }

    private void requireTeacherCourse(Long courseId, Long teacherId) {
        CourseBrief course = findCourse(courseId);
        if (course == null || !Objects.equals(course.teacherId(), teacherId)) {
            throw new UserErrorException(HttpStatus.FORBIDDEN, "只能批改自己课程的学生练习");
        }
    }

    private LearningSubmissionPO requireTeacherSubmission(Long submissionId, UserInfoDTO user) {
        LearningSubmissionPO submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "学生提交不存在");
        }
        LearningPracticePO practice = requirePractice(submission.getPracticeId());
        requireTeacherCourse(practice.getCourseId(), user.getUserId());
        return submission;
    }

    private LearningPracticePO requirePractice(Long practiceId) {
        LearningPracticePO practice = practiceMapper.selectById(practiceId);
        if (practice == null || !Objects.equals(practice.getStatus(), 1)) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "练习不存在或暂未开放");
        }
        return practice;
    }

    private List<LearningQuestionPO> questionsOf(Long practiceId) {
        return questionMapper.selectList(new LambdaQueryWrapper<LearningQuestionPO>()
                .eq(LearningQuestionPO::getPracticeId, practiceId)
                .orderByAsc(LearningQuestionPO::getSortOrder));
    }

    private LearningSubmissionPO findSubmission(Long practiceId, Long studentId) {
        return submissionMapper.selectOne(new LambdaQueryWrapper<LearningSubmissionPO>()
                .eq(LearningSubmissionPO::getPracticeId, practiceId)
                .eq(LearningSubmissionPO::getStudentId, studentId));
    }

    private UserInfoDTO requireRole(String expectedRole) {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new UserErrorException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (!expectedRole.equals(user.getRoleCode())) {
            throw new UserErrorException(HttpStatus.FORBIDDEN, "当前账号没有此功能权限");
        }
        return user;
    }

    private int calculateAutoScore(List<LearningQuestionPO> questions, Map<Long, String> answers) {
        return questions.stream()
                .filter(question -> "SINGLE".equals(question.getQuestionType()))
                .filter(question -> normalizeSingleChoice(question.getReferenceAnswer())
                        .equals(normalizeSingleChoice(answers.get(question.getId()))))
                .mapToInt(question -> question.getQuestionScore() == null ? 0 : question.getQuestionScore())
                .sum();
    }

    private int awardedScore(
            LearningQuestionPO question,
            String answer,
            Map<Long, QuestionReviewDetail> questionReviews
    ) {
        if ("SINGLE".equals(question.getQuestionType())) {
            return normalizeSingleChoice(question.getReferenceAnswer()).equals(normalizeSingleChoice(answer))
                    ? (question.getQuestionScore() == null ? 0 : question.getQuestionScore())
                    : 0;
        }
        QuestionReviewDetail review = questionReviews.get(question.getId());
        return review == null ? 0 : review.score();
    }

    private Map<Long, QuestionReviewDetail> reviewDetailsFor(
            LearningSubmissionPO submission,
            List<LearningQuestionPO> questions
    ) {
        if (submission == null) return Map.of();
        Map<Long, QuestionReviewDetail> reviews = new LinkedHashMap<>(
                readQuestionReviews(submission.getQuestionReviewJson()));
        if (!reviews.isEmpty() || !"REVIEWED".equals(submission.getStatus())) return reviews;

        List<LearningQuestionPO> openQuestions = questions.stream()
                .filter(question -> "SHORT".equals(question.getQuestionType()))
                .toList();
        if (openQuestions.size() == 1) {
            LearningQuestionPO question = openQuestions.getFirst();
            int manualScore = Math.max(0,
                    (submission.getTeacherScore() == null ? 0 : submission.getTeacherScore())
                            - (submission.getAutoScore() == null ? 0 : submission.getAutoScore()));
            int maxScore = question.getQuestionScore() == null ? 0 : question.getQuestionScore();
            reviews.put(question.getId(), new QuestionReviewDetail(
                    Math.min(manualScore, maxScore),
                    submission.getTeacherFeedback(),
                    "TEACHER",
                    null,
                    null
            ));
        }
        return reviews;
    }

    private int displayedScore(LearningSubmissionPO submission) {
        return submission.getTeacherScore() == null ? submission.getAutoScore() : submission.getTeacherScore();
    }

    private List<String> readOptions(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Map<Long, String> readAnswers(String json) {
        if (!StringUtils.hasText(json)) return Map.of();
        try {
            Map<String, String> raw = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
            Map<Long, String> result = new LinkedHashMap<>();
            raw.forEach((key, value) -> result.put(Long.valueOf(key), value));
            return result;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String writeAnswers(Map<Long, String> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (Exception exception) {
            throw new UserErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "答案保存失败");
        }
    }

    private Map<Long, QuestionReviewDetail> readQuestionReviews(String json) {
        if (!StringUtils.hasText(json)) return Map.of();
        try {
            Map<String, QuestionReviewDetail> raw = objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, QuestionReviewDetail>>() {}
            );
            Map<Long, QuestionReviewDetail> result = new LinkedHashMap<>();
            raw.forEach((key, value) -> result.put(Long.valueOf(key), value));
            return result;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String writeQuestionReviews(Map<Long, QuestionReviewDetail> reviews) {
        try {
            return objectMapper.writeValueAsString(reviews);
        } catch (Exception exception) {
            throw new UserErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "逐题批改结果保存失败");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    // Accept older saved answers such as "1" while preventing invalid characters from entering new records.
    private String normalizeSingleChoice(String value) {
        String normalized = normalize(value);
        if (normalized.matches("[A-D]")) return normalized;
        if (normalized.matches("[1-4]")) {
            return String.valueOf((char) ('A' + Integer.parseInt(normalized) - 1));
        }
        return "";
    }

    private String displayAnswer(LearningQuestionPO question, String answer) {
        return "SINGLE".equals(question.getQuestionType()) ? normalizeSingleChoice(answer) : answer;
    }

    private String writeOptions(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception exception) {
            throw new UserErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "题目选项保存失败");
        }
    }

    private CourseBrief findCourse(Long courseId) {
        List<CourseBrief> courses = jdbcTemplate.query(
                "SELECT id, course_name, teacher_id FROM edu_course WHERE id = ?",
                (resultSet, rowNum) -> new CourseBrief(
                        resultSet.getLong("id"),
                        resultSet.getString("course_name"),
                        resultSet.getObject("teacher_id", Long.class)
                ),
                courseId
        );
        return courses.isEmpty() ? null : courses.getFirst();
    }

    private record CourseBrief(Long id, String title, Long teacherId) {
    }

    private record QuestionReviewDetail(
            Integer score,
            String feedback,
            String source,
            String reasoning,
            BigDecimal confidence
    ) {
    }
}
