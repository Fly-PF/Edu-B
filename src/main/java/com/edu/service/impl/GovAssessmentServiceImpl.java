package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.exception.BaseException;
import com.edu.mapper.GovPracticeAnswerMapper;
import com.edu.mapper.GovPracticeRecordMapper;
import com.edu.mapper.GovQuestionMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.gov.GovMockExamCreateRequest;
import com.edu.pojo.dto.gov.GovMockExamSubmitRequest;
import com.edu.pojo.dto.gov.GovQuestionContentDTO;
import com.edu.pojo.po.gov.GovPracticeAnswerPO;
import com.edu.pojo.po.gov.GovPracticeRecordPO;
import com.edu.pojo.po.gov.GovQuestionPO;
import com.edu.pojo.vo.gov.GovMockExamRecordVO;
import com.edu.pojo.vo.gov.GovMockExamReportVO;
import com.edu.pojo.vo.gov.GovMockExamVO;
import com.edu.pojo.vo.gov.GovMockQuestionVO;
import com.edu.pojo.vo.gov.GovMockSubjectBreakdownVO;
import com.edu.pojo.vo.gov.GovWrongQuestionVO;
import com.edu.service.GovAssessmentService;
import com.edu.util.SecurityUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GovAssessmentServiceImpl implements GovAssessmentService {
    private static final Set<String> ALLOWED_SUBJECTS = Set.of(
            "政治理论",
            "常识判断",
            "语言理解与表达",
            "数量关系",
            "判断推理",
            "资料分析"
    );

    private static final String PRACTICE_MODE_MOCK = "MOCK";
    private static final String STATUS_DOING = "DOING";
    private static final String STATUS_FINISHED = "FINISHED";
    private static final int PUBLISHED = 1;
    private static final int NOT_DELETED = 0;

    private final GovQuestionMapper questionMapper;
    private final GovPracticeRecordMapper recordMapper;
    private final GovPracticeAnswerMapper answerMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public GovMockExamVO createMockExam(GovMockExamCreateRequest request) {
        UserInfoDTO user = requireUser();
        validateCreateRequest(request);

        List<GovQuestionPO> selectedQuestions = drawQuestions(request);
        LocalDateTime now = LocalDateTime.now();
        GovPracticeRecordPO record = GovPracticeRecordPO.builder()
                .userId(user.getUserId())
                .practiceMode(PRACTICE_MODE_MOCK)
                .subject(normalizeSubject(request.getSubject()))
                .totalCount(selectedQuestions.size())
                .correctCount(0)
                .durationLimitSeconds(request.getDurationLimitSeconds())
                .score(BigDecimal.ZERO)
                .status(STATUS_DOING)
                .startedAt(now)
                .createTime(now)
                .updateTime(now)
                .deleted(NOT_DELETED)
                .build();
        recordMapper.insert(record);
        if (record.getId() == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "模拟考试记录创建失败");
        }

        List<GovPracticeAnswerPO> answerRows = new ArrayList<>();
        for (int index = 0; index < selectedQuestions.size(); index++) {
            GovQuestionPO question = selectedQuestions.get(index);
            answerRows.add(GovPracticeAnswerPO.builder()
                    .practiceId(record.getId())
                    .questionId(question.getId())
                    .questionOrder(index + 1)
                    .selectedAnswerJson("[]")
                    .isCorrect(0)
                    .createTime(now)
                    .updateTime(now)
                    .deleted(NOT_DELETED)
                    .build());
        }
        answerRows.forEach(answerMapper::insert);

        return toExamView(record, selectedQuestions, answerRows);
    }

    @Override
    @Transactional(readOnly = true)
    public GovMockExamVO getMockExam(Long practiceId) {
        UserInfoDTO user = requireUser();
        GovPracticeRecordPO record = requireRecord(practiceId, user);
        List<GovPracticeAnswerPO> answerRows = loadAnswers(practiceId);
        List<GovQuestionPO> questions = loadQuestions(answerRows);
        return toExamView(record, questions, answerRows);
    }

    @Override
    @Transactional
    public GovMockExamReportVO submitMockExam(Long practiceId, GovMockExamSubmitRequest request) {
        UserInfoDTO user = requireUser();
        GovPracticeRecordPO record = requireRecord(practiceId, user);
        List<GovPracticeAnswerPO> answerRows = loadAnswers(practiceId);
        List<GovQuestionPO> questions = loadQuestions(answerRows);

        if (STATUS_FINISHED.equals(record.getStatus())) {
            return buildReport(record, answerRows, questions);
        }

        Map<Long, GovMockExamSubmitRequest.AnswerItem> submittedAnswers = resolveSubmittedAnswers(request);
        Map<Long, GovQuestionPO> questionById = questions.stream()
                .collect(Collectors.toMap(GovQuestionPO::getId, Function.identity(), (left, right) -> left));
        LocalDateTime finishedAt = LocalDateTime.now();
        int correctCount = 0;

        for (GovPracticeAnswerPO answerRow : answerRows) {
            GovQuestionPO question = Objects.requireNonNull(
                    questionById.get(answerRow.getQuestionId()),
                    () -> "模拟考试题目不存在"
            );
            GovQuestionContentDTO content = parseContent(question);
            List<String> selectedAnswers = normalizeAnswers(
                    submittedAnswers.containsKey(answerRow.getQuestionId())
                            ? submittedAnswers.get(answerRow.getQuestionId()).getSelectedAnswers()
                            : List.of()
            );
            boolean correct = isAnswerCorrect(selectedAnswers, content.getAnswer());

            answerRow.setSelectedAnswerJson(writeJson(selectedAnswers));
            answerRow.setIsCorrect(correct ? 1 : 0);
            answerRow.setUpdateTime(finishedAt);
            answerMapper.updateById(answerRow);

            if (correct) {
                correctCount++;
            }
        }

        BigDecimal score = calculateRate(correctCount, questions.size());
        int durationUsedSeconds = calculateDurationSeconds(record.getStartedAt(), finishedAt);
        record.setCorrectCount(correctCount);
        record.setScore(score);
        record.setStatus(STATUS_FINISHED);
        record.setFinishedAt(finishedAt);
        record.setUpdateTime(finishedAt);
        recordMapper.updateById(record);

        return buildReport(record, answerRows, questions);
    }

    @Override
    @Transactional(readOnly = true)
    public GovMockExamReportVO getMockExamReport(Long practiceId) {
        UserInfoDTO user = requireUser();
        GovPracticeRecordPO record = requireRecord(practiceId, user);
        if (!STATUS_FINISHED.equals(record.getStatus())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "本次模拟考试尚未交卷");
        }

        List<GovPracticeAnswerPO> answerRows = loadAnswers(practiceId);
        List<GovQuestionPO> questions = loadQuestions(answerRows);
        return buildReport(record, answerRows, questions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovMockExamRecordVO> listMockExamRecords() {
        UserInfoDTO user = requireUser();
        return recordMapper.selectList(new LambdaQueryWrapper<GovPracticeRecordPO>()
                        .eq(GovPracticeRecordPO::getUserId, user.getUserId())
                        .eq(GovPracticeRecordPO::getPracticeMode, PRACTICE_MODE_MOCK)
                        .eq(GovPracticeRecordPO::getDeleted, NOT_DELETED)
                        .orderByDesc(GovPracticeRecordPO::getCreateTime)
                        .orderByDesc(GovPracticeRecordPO::getId)
                        .last("LIMIT 20"))
                .stream()
                .map(this::toRecordView)
                .toList();
    }

    private void validateCreateRequest(GovMockExamCreateRequest request) {
        if (request.getQuestionCount() == null
                || request.getQuestionCount() < 1
                || request.getQuestionCount() > 100) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "题目数量必须在 1 到 100 之间");
        }
        if (request.getDurationLimitSeconds() == null
                || request.getDurationLimitSeconds() < 60
                || request.getDurationLimitSeconds() > 7200) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "考试时长必须在 1 到 120 分钟之间");
        }
        if (request.getDifficulty() != null
                && (request.getDifficulty() < 1 || request.getDifficulty() > 5)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "题目难度必须在 1 到 5 之间");
        }
        if (StringUtils.hasText(request.getSubject())
                && !ALLOWED_SUBJECTS.contains(request.getSubject().trim())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "不支持的科目范围");
        }
    }

    private List<GovQuestionPO> drawQuestions(GovMockExamCreateRequest request) {
        List<GovQuestionPO> candidates = questionMapper.selectList(new LambdaQueryWrapper<GovQuestionPO>()
                .eq(GovQuestionPO::getStatus, PUBLISHED)
                .eq(GovQuestionPO::getDeleted, NOT_DELETED)
                .eq(StringUtils.hasText(request.getSubject()), GovQuestionPO::getSubject, normalizeSubject(request.getSubject()))
                .eq(request.getDifficulty() != null, GovQuestionPO::getDifficulty, request.getDifficulty())
                .orderByAsc(GovQuestionPO::getId)
                .last("LIMIT 1000"));

        if (candidates.size() < request.getQuestionCount()) {
            throw new BaseException(
                    HttpStatus.BAD_REQUEST,
                    "当前题库可用题目不足，至少需要 " + request.getQuestionCount() + " 道题"
            );
        }

        Collections.shuffle(candidates);
        return new ArrayList<>(candidates.subList(0, request.getQuestionCount()));
    }

    private GovMockExamVO toExamView(
            GovPracticeRecordPO record,
            List<GovQuestionPO> questions,
            List<GovPracticeAnswerPO> answerRows
    ) {
        Map<Long, GovPracticeAnswerPO> answerByQuestion = answerRows.stream()
                .collect(Collectors.toMap(
                        GovPracticeAnswerPO::getQuestionId,
                        Function.identity(),
                        (left, right) -> left
                ));

        List<GovMockQuestionVO> questionViews = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            GovQuestionPO question = questions.get(index);
            GovPracticeAnswerPO answerRow = answerByQuestion.get(question.getId());
            GovQuestionContentDTO content = parseContent(question);
            content.setAnswer(List.of());
            content.setAnalysis(null);

            questionViews.add(GovMockQuestionVO.builder()
                    .questionId(question.getId())
                    .questionOrder(answerRow == null ? index + 1 : answerRow.getQuestionOrder())
                    .questionType(question.getQuestionType())
                    .subject(question.getSubject())
                    .difficulty(question.getDifficulty())
                    .content(content)
                    .build());
        }

        int answeredCount = (int) answerRows.stream().filter(this::hasAnswer).count();
        return GovMockExamVO.builder()
                .practiceId(record.getId())
                .status(record.getStatus())
                .subject(record.getSubject())
                .totalCount(questions.size())
                .answeredCount(answeredCount)
                .durationLimitSeconds(record.getDurationLimitSeconds())
                .startedAt(record.getStartedAt())
                .finishedAt(record.getFinishedAt())
                .questions(questionViews)
                .build();
    }

    private GovMockExamReportVO buildReport(
            GovPracticeRecordPO record,
            List<GovPracticeAnswerPO> answerRows,
            List<GovQuestionPO> questions
    ) {
        Map<Long, GovQuestionPO> questionById = questions.stream()
                .collect(Collectors.toMap(GovQuestionPO::getId, Function.identity(), (left, right) -> left));
        Map<String, SubjectStat> subjectStats = new LinkedHashMap<>();
        List<GovWrongQuestionVO> wrongQuestions = new ArrayList<>();

        for (GovPracticeAnswerPO answerRow : answerRows) {
            GovQuestionPO question = questionById.get(answerRow.getQuestionId());
            if (question == null) {
                continue;
            }

            String subject = question.getSubject() == null ? "综合" : question.getSubject();
            SubjectStat stat = subjectStats.computeIfAbsent(subject, key -> new SubjectStat());
            stat.totalCount++;
            if (Integer.valueOf(1).equals(answerRow.getIsCorrect())) {
                stat.correctCount++;
            } else {
                GovQuestionContentDTO content = parseContent(question);
                wrongQuestions.add(GovWrongQuestionVO.builder()
                        .questionId(question.getId())
                        .questionOrder(answerRow.getQuestionOrder())
                        .questionType(question.getQuestionType())
                        .subject(question.getSubject())
                        .difficulty(question.getDifficulty())
                        .content(content)
                        .selectedAnswers(parseSelectedAnswers(answerRow.getSelectedAnswerJson()))
                        .correctAnswers(normalizeAnswers(content.getAnswer()))
                        .analysis(content.getAnalysis())
                        .build());
            }
        }

        List<GovMockSubjectBreakdownVO> breakdown = subjectStats.entrySet().stream()
                .map(entry -> GovMockSubjectBreakdownVO.builder()
                        .subject(entry.getKey())
                        .totalCount(entry.getValue().totalCount)
                        .correctCount(entry.getValue().correctCount)
                        .accuracyRate(calculateRate(entry.getValue().correctCount, entry.getValue().totalCount))
                        .build())
                .toList();

        int totalCount = questions.size();
        int correctCount = record.getCorrectCount() == null ? 0 : record.getCorrectCount();
        return GovMockExamReportVO.builder()
                .practiceId(record.getId())
                .subject(record.getSubject())
                .status(record.getStatus())
                .totalCount(totalCount)
                .correctCount(correctCount)
                .score(record.getScore() == null ? BigDecimal.ZERO : record.getScore())
                .accuracyRate(calculateRate(correctCount, totalCount))
                .durationUsedSeconds(calculateDurationSeconds(record.getStartedAt(), record.getFinishedAt()))
                .durationLimitSeconds(record.getDurationLimitSeconds())
                .startedAt(record.getStartedAt())
                .finishedAt(record.getFinishedAt())
                .subjectBreakdown(breakdown)
                .wrongQuestions(wrongQuestions)
                .build();
    }

    private List<GovPracticeAnswerPO> loadAnswers(Long practiceId) {
        return answerMapper.selectList(new LambdaQueryWrapper<GovPracticeAnswerPO>()
                .eq(GovPracticeAnswerPO::getPracticeId, practiceId)
                .eq(GovPracticeAnswerPO::getDeleted, NOT_DELETED)
                .orderByAsc(GovPracticeAnswerPO::getQuestionOrder)
                .orderByAsc(GovPracticeAnswerPO::getId));
    }

    private List<GovQuestionPO> loadQuestions(List<GovPracticeAnswerPO> answerRows) {
        if (answerRows.isEmpty()) {
            return List.of();
        }

        List<Long> questionIds = answerRows.stream()
                .map(GovPracticeAnswerPO::getQuestionId)
                .distinct()
                .toList();
        Map<Long, GovQuestionPO> questionById = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(GovQuestionPO::getId, Function.identity(), (left, right) -> left));

        return answerRows.stream()
                .map(answerRow -> Objects.requireNonNull(
                        questionById.get(answerRow.getQuestionId()),
                        () -> "模拟考试题目不存在"
                ))
                .toList();
    }

    private GovPracticeRecordPO requireRecord(Long practiceId, UserInfoDTO user) {
        GovPracticeRecordPO record = recordMapper.selectOne(new LambdaQueryWrapper<GovPracticeRecordPO>()
                .eq(GovPracticeRecordPO::getId, practiceId)
                .eq(GovPracticeRecordPO::getDeleted, NOT_DELETED));
        if (record == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "模拟考试记录不存在");
        }
        if (!user.getUserId().equals(record.getUserId())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权访问该模拟考试记录");
        }
        return record;
    }

    private UserInfoDTO requireUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private Map<Long, GovMockExamSubmitRequest.AnswerItem> resolveSubmittedAnswers(GovMockExamSubmitRequest request) {
        if (request == null || request.getAnswers() == null) {
            return Map.of();
        }
        return request.getAnswers().stream()
                .filter(item -> item != null && item.getQuestionId() != null)
                .collect(Collectors.toMap(
                        GovMockExamSubmitRequest.AnswerItem::getQuestionId,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private List<String> normalizeAnswers(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase())
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    private boolean isAnswerCorrect(List<String> selectedAnswers, List<String> correctAnswers) {
        return selectedAnswers.equals(normalizeAnswers(correctAnswers));
    }

    private boolean hasAnswer(GovPracticeAnswerPO answerRow) {
        return !parseSelectedAnswers(answerRow.getSelectedAnswerJson()).isEmpty();
    }

    private List<String> parseSelectedAnswers(String selectedAnswerJson) {
        if (!StringUtils.hasText(selectedAnswerJson) || "[]".equals(selectedAnswerJson.trim())) {
            return List.of();
        }
        try {
            return normalizeAnswers(objectMapper.readValue(selectedAnswerJson, new TypeReference<>() {
            }));
        } catch (Exception ex) {
            return List.of();
        }
    }

    private GovQuestionContentDTO parseContent(GovQuestionPO question) {
        try {
            return objectMapper.readValue(question.getContentJson(), GovQuestionContentDTO.class);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "题目内容格式错误");
        }
    }

    private String writeJson(List<String> selectedAnswers) {
        try {
            return objectMapper.writeValueAsString(selectedAnswers);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "答案保存失败");
        }
    }

    private BigDecimal calculateRate(int correctCount, int totalCount) {
        if (totalCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(correctCount * 100L)
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
    }

    private int calculateDurationSeconds(LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (startedAt == null || finishedAt == null || finishedAt.isBefore(startedAt)) {
            return 0;
        }
        long seconds = Duration.between(startedAt, finishedAt).getSeconds();
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }

    private String normalizeSubject(String subject) {
        return StringUtils.hasText(subject) ? subject.trim() : null;
    }

    private GovMockExamRecordVO toRecordView(GovPracticeRecordPO record) {
        return GovMockExamRecordVO.builder()
                .practiceId(record.getId())
                .subject(record.getSubject())
                .totalCount(record.getTotalCount())
                .correctCount(record.getCorrectCount())
                .score(record.getScore())
                .status(record.getStatus())
                .durationLimitSeconds(record.getDurationLimitSeconds())
                .startedAt(record.getStartedAt())
                .finishedAt(record.getFinishedAt())
                .build();
    }

    private static final class SubjectStat {
        private int totalCount;
        private int correctCount;
    }
}

