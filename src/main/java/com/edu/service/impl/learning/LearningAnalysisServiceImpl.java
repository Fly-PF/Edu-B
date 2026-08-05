package com.edu.service.impl.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.exception.BaseException;
import com.edu.learninganalysis.CourseAssistantGateway;
import com.edu.learninganalysis.LearningRiskModel;
import com.edu.mapper.learning.LearningAiTraceMapper;
import com.edu.mapper.learning.LearningCaseMapper;
import com.edu.mapper.learning.LearningEvidenceMapper;
import com.edu.mapper.learning.LearningPlanMapper;
import com.edu.mapper.learning.LearningRecommendationMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.learning.LearningCaseGenerateRequest;
import com.edu.pojo.dto.learning.LearningAssistantRequest;
import com.edu.pojo.dto.learning.LearningEvidenceSubmitRequest;
import com.edu.pojo.dto.learning.LearningPlanDecisionRequest;
import com.edu.pojo.dto.learning.LearningPlanReviewRequest;
import com.edu.pojo.po.EduChapterPO;
import com.edu.pojo.po.EduClassPO;
import com.edu.pojo.po.EduClassStudentPO;
import com.edu.pojo.po.EduCourseClassPO;
import com.edu.pojo.po.EduCoursePO;
import com.edu.pojo.po.EduStudyRecordPO;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.po.learning.LearningAiTracePO;
import com.edu.pojo.po.learning.LearningCasePO;
import com.edu.pojo.po.learning.LearningEvidencePO;
import com.edu.pojo.po.learning.LearningPlanPO;
import com.edu.pojo.po.learning.LearningRecommendationPO;
import com.edu.pojo.vo.learning.LearningCourseProfileVO;
import com.edu.pojo.vo.learning.LearningCourseRecommendationVO;
import com.edu.pojo.vo.learning.LearningAbilityProfileVO;
import com.edu.pojo.vo.learning.LearningAssistantReplyVO;
import com.edu.pojo.vo.learning.LearningClassTrendVO;
import com.edu.pojo.vo.learning.LearningGrowthCaseVO;
import com.edu.pojo.vo.learning.LearningRiskAlertVO;
import com.edu.pojo.vo.learning.LearningStudentAbilityVO;
import com.edu.pojo.vo.learning.LearningStudentGrowthVO;
import com.edu.pojo.vo.learning.LearningStudentOverviewVO;
import com.edu.pojo.vo.learning.LearningStudentTypeProfileVO;
import com.edu.pojo.vo.learning.LearningTeacherGrowthVO;
import com.edu.pojo.vo.learning.LearningTeacherOverviewVO;
import com.edu.pojo.vo.course.ChapterResourceProgressVO;
import com.edu.repository.EduChapterRepository;
import com.edu.repository.EduClassRepository;
import com.edu.repository.EduClassStudentRepository;
import com.edu.repository.EduCourseClassRepository;
import com.edu.repository.EduCourseRepository;
import com.edu.repository.EduStudyRecordRepository;
import com.edu.repository.SysUserRepository;
import com.edu.service.learning.LearningAnalysisService;
import com.edu.service.CourseResourceProgressService;
import com.edu.util.SecurityUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A closed loop for learning support: observable behaviour -> a teacher-owned
 * micro-plan -> student evidence -> AI-assisted and teacher-confirmed review.
 */
@Service
@RequiredArgsConstructor
public class LearningAnalysisServiceImpl implements LearningAnalysisService {
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String CASE_DRAFT = "DRAFT";
    private static final String CASE_PUBLISHED = "PUBLISHED";
    private static final String CASE_EVIDENCE_SUBMITTED = "EVIDENCE_SUBMITTED";
    private static final String CASE_EFFECTIVE = "EFFECTIVE";
    private static final String CASE_CONTINUE = "CONTINUE";
    private static final String CASE_ESCALATE = "ESCALATE";
    private static final String CASE_REJECTED = "REJECTED";
    private static final String PLAN_DRAFT = "DRAFT";
    private static final String PLAN_PUBLISHED = "PUBLISHED";
    private static final String PLAN_EVIDENCE_SUBMITTED = "EVIDENCE_SUBMITTED";
    private static final String PLAN_REVIEWED = "REVIEWED";
    private static final String PLAN_REJECTED = "REJECTED";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EduClassRepository classRepository;
    private final EduClassStudentRepository classStudentRepository;
    private final EduCourseClassRepository courseClassRepository;
    private final EduCourseRepository courseRepository;
    private final EduChapterRepository chapterRepository;
    private final EduStudyRecordRepository studyRecordRepository;
    private final SysUserRepository userRepository;
    private final LearningCaseMapper caseMapper;
    private final LearningPlanMapper planMapper;
    private final LearningRecommendationMapper recommendationMapper;
    private final LearningEvidenceMapper evidenceMapper;
    private final LearningAiTraceMapper traceMapper;
    private final CourseAssistantGateway courseAssistantGateway;
    private final ObjectMapper objectMapper;
    private final CourseResourceProgressService resourceProgressService;
    private final LearningRiskModel riskModel = new LearningRiskModel();

    @Override
    public LearningTeacherGrowthVO getTeacherDashboard(Long classId) {
        EduClassPO clazz = requireTeacherClass(classId);
        ClassAnalysis analysis = analyseClass(clazz);
        List<LearningCasePO> cases = caseMapper.selectList(new LambdaQueryWrapper<LearningCasePO>()
                .eq(LearningCasePO::getClassId, classId)
                .orderByDesc(LearningCasePO::getUpdatedAt)
                .orderByDesc(LearningCasePO::getId));
        List<LearningGrowthCaseVO> caseViews = cases.stream().map(this::toCaseView).toList();

        int pending = (int) caseViews.stream().filter(item -> "PENDING".equals(item.getTeacherDecision())).count();
        int awaitingReview = (int) caseViews.stream().filter(item -> CASE_EVIDENCE_SUBMITTED.equals(item.getStatus())).count();
        int effective = (int) caseViews.stream().filter(item -> CASE_EFFECTIVE.equals(item.getStatus())).count();
        List<CourseSnapshot> allSnapshots = analysis.snapshotsByStudent().values().stream()
                .flatMap(Collection::stream).toList();
        List<LearningStudentAbilityVO> studentAbilities = buildStudentAbilities(analysis);
        List<LearningRiskAlertVO> riskAlerts = analysis.risks().stream()
                .filter(item -> !"LOW".equals(item.getRiskLevel()))
                .map(this::toRiskAlert).toList();
        return LearningTeacherGrowthVO.builder()
                .classId(clazz.getId())
                .className(clazz.getClassName())
                .grade(clazz.getGrade())
                .modelName("能力画像由真实进度、时长和实践记录计算；大模型仅基于这些证据答疑")
                .summary(LearningTeacherGrowthVO.Summary.builder()
                        .studentCount(analysis.students().size())
                        .courseCount(analysis.courseCount())
                        .averageProgress(analysis.averageProgress())
                        .activeStudents(analysis.activeStudents())
                        .attentionItems((int) analysis.risks().stream().filter(item -> !"LOW".equals(item.getRiskLevel())).count())
                        .pendingDecisions(pending)
                        .awaitingReview(awaitingReview)
                        .effectiveCases(effective)
                        .build())
                .risks(analysis.risks())
                .classProfile(analysis.classProfile())
                .classAbilityProfile(buildAbilityProfile(allSnapshots))
                .classTrend(buildClassTrend(analysis.students()))
                .studentProfiles(analysis.studentProfiles())
                .studentAbilities(studentAbilities)
                .riskAlerts(riskAlerts)
                .cases(caseViews)
                .build();
    }

    @Override
    @Transactional
    public LearningGrowthCaseVO generateCase(LearningCaseGenerateRequest request) {
        UserInfoDTO teacher = requireTeacher();
        EduClassPO clazz = requireTeacherClass(request.getClassId());
        CourseSnapshot snapshot = requireSnapshot(clazz, request.getCourseId(), request.getStudentId());
        Optional<LearningCasePO> existing = caseMapper.selectList(new LambdaQueryWrapper<LearningCasePO>()
                        .eq(LearningCasePO::getClassId, request.getClassId())
                        .eq(LearningCasePO::getCourseId, request.getCourseId())
                        .eq(LearningCasePO::getStudentId, request.getStudentId())
                        .notIn(LearningCasePO::getStatus, List.of(CASE_EFFECTIVE, CASE_ESCALATE, CASE_REJECTED))
                        .orderByDesc(LearningCasePO::getUpdatedAt)
                        .last("LIMIT 1"))
                .stream().findFirst();
        if (existing.isPresent()) {
            return toCaseView(existing.get());
        }

        int classAverage = classCourseAverage(clazz.getId(), snapshot.course(), snapshot.assignment(), snapshot.chapters());
        LearningRiskModel.LearningRiskResult risk = assess(snapshot, classAverage);
        String courseContext = buildCourseContext(snapshot);
        String riskSummary = buildRiskSummary(snapshot, classAverage, risk);
        long started = System.currentTimeMillis();
        Optional<CourseAssistantGateway.PlanResponse> aiPlan = courseAssistantGateway.generatePlan(
                new CourseAssistantGateway.PlanRequest(courseContext, riskSummary, snapshot.nextChapter())
        );
        long elapsed = System.currentTimeMillis() - started;
        PlanDraft planDraft = aiPlan.map(item -> new PlanDraft(
                        item.diagnosis(), item.goal(), trimList(item.taskSteps(), 4), item.durationMinutes(),
                        item.acceptanceCriteria(), item.checkQuestion(), trimList(item.expectedSignals(), 4), "MODEL", item.modelName()
                ))
                .orElseGet(() -> fallbackPlan(snapshot, risk));
        validatePlan(planDraft);

        LocalDateTime now = LocalDateTime.now();
        LearningCasePO learningCase = LearningCasePO.builder()
                .classId(clazz.getId())
                .courseId(snapshot.course().getId())
                .chapterId(snapshot.nextChapterId())
                .studentId(snapshot.studentId())
                .teacherId(teacher.getUserId())
                .riskScore(risk.score())
                .riskLevel(risk.level())
                .behaviorSnapshot(serializeSnapshot(snapshot, classAverage))
                .diagnosis(planDraft.diagnosis())
                .diagnosisSource(planDraft.source())
                .modelName(planDraft.modelName())
                .status(CASE_DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();
        caseMapper.insert(learningCase);
        LearningPlanPO plan = LearningPlanPO.builder()
                .caseId(learningCase.getId())
                .title(buildPlanTitle(snapshot, planDraft.goal()))
                .learningGoal(planDraft.goal())
                .taskSteps(serializeList(planDraft.steps()))
                .durationMinutes(planDraft.durationMinutes())
                .acceptanceCriteria(planDraft.acceptanceCriteria())
                .checkQuestion(planDraft.checkQuestion())
                .expectedSignals(serializeList(planDraft.expectedSignals()))
                .teacherDecision("PENDING")
                .status(PLAN_DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();
        planMapper.insert(plan);
        saveTrace(learningCase.getId(), plan.getId(), snapshot.studentId(), "PLAN_GENERATION", planDraft.modelName(),
                planDraft.source(), summarizeContext(snapshot), elapsed);
        return toCaseView(learningCase, plan, null, snapshot, classAverage);
    }

    @Override
    @Transactional
    public LearningGrowthCaseVO decidePlan(Long caseId, LearningPlanDecisionRequest request) {
        LearningCasePO learningCase = requireCase(caseId);
        requireTeacherClass(learningCase.getClassId());
        LearningPlanPO plan = requireLatestPlan(caseId);
        String decision = cleanDecision(request.getDecision());
        LocalDateTime now = LocalDateTime.now();
        if ("REJECT".equals(decision)) {
            plan.setTeacherDecision("REJECTED");
            plan.setStatus(PLAN_REJECTED);
            learningCase.setStatus(CASE_REJECTED);
        } else {
            if ("EDIT".equals(decision)) {
                applyTeacherEdits(plan, request);
                plan.setTeacherDecision("EDITED");
            } else {
                plan.setTeacherDecision("ADOPTED");
            }
            plan.setStatus(PLAN_PUBLISHED);
            learningCase.setStatus(CASE_PUBLISHED);
        }
        plan.setUpdatedAt(now);
        learningCase.setUpdatedAt(now);
        planMapper.updateById(plan);
        caseMapper.updateById(learningCase);
        return toCaseView(learningCase, plan, latestEvidence(plan.getId()), null, null);
    }

    @Override
    @Transactional
    public LearningGrowthCaseVO submitEvidence(Long planId, LearningEvidenceSubmitRequest request) {
        UserInfoDTO student = requireStudent();
        LearningPlanPO plan = requirePlan(planId);
        LearningCasePO learningCase = requireCase(plan.getCaseId());
        if (!Objects.equals(learningCase.getStudentId(), student.getUserId())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权提交该学习计划的证据");
        }
        if (!Set.of(PLAN_PUBLISHED, PLAN_EVIDENCE_SUBMITTED).contains(plan.getStatus())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "该计划尚未由教师确认或已经结束");
        }
        String answer = cleanText(request.getAnswer(), 2000);
        long started = System.currentTimeMillis();
        Optional<CourseAssistantGateway.EvidenceResponse> aiResult = courseAssistantGateway.assessEvidence(
                new CourseAssistantGateway.EvidenceRequest(
                        buildCourseContext(requireSnapshot(learningCase)), plan.getCheckQuestion(), answer
                )
        );
        long elapsed = System.currentTimeMillis() - started;
        EvidenceDraft assessment = aiResult.map(item -> new EvidenceDraft(
                        item.result(), item.assessment(), item.confidence(), "MODEL", item.modelName()
                ))
                .orElseGet(() -> fallbackEvidence(answer));
        validateEvidenceAssessment(assessment);

        LocalDateTime now = LocalDateTime.now();
        LearningEvidencePO evidence = LearningEvidencePO.builder()
                .planId(planId)
                .studentId(student.getUserId())
                .reflection(cleanText(request.getReflection(), 2000))
                .difficulty(cleanText(request.getDifficulty(), 1000))
                .answer(answer)
                .aiAssessment(assessment.assessment())
                .confidence(assessment.confidence())
                .result(assessment.result())
                .assessmentSource(assessment.source())
                .submittedAt(now)
                .build();
        evidenceMapper.insert(evidence);
        plan.setStatus(PLAN_EVIDENCE_SUBMITTED);
        plan.setUpdatedAt(now);
        learningCase.setStatus(CASE_EVIDENCE_SUBMITTED);
        learningCase.setUpdatedAt(now);
        planMapper.updateById(plan);
        caseMapper.updateById(learningCase);
        saveTrace(learningCase.getId(), planId, student.getUserId(), "EVIDENCE_ASSESSMENT", assessment.modelName(),
                assessment.source(), "理解检查：" + abbreviate(plan.getCheckQuestion(), 180), elapsed);
        return toCaseView(learningCase, plan, evidence, null, null);
    }

    @Override
    @Transactional
    public LearningGrowthCaseVO reviewPlan(Long planId, LearningPlanReviewRequest request) {
        LearningPlanPO plan = requirePlan(planId);
        LearningCasePO learningCase = requireCase(plan.getCaseId());
        requireTeacherClass(learningCase.getClassId());
        LearningEvidencePO evidence = latestEvidence(planId);
        if (evidence == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "学生尚未提交学习证据，不能完成复评");
        }
        String outcome = cleanOutcome(request.getOutcome());
        LocalDateTime now = LocalDateTime.now();
        evidence.setTeacherConclusion(cleanText(defaultString(request.getConclusion(), defaultConclusion(outcome)), 1000));
        evidence.setReviewedAt(now);
        learningCase.setStatus(outcome);
        learningCase.setUpdatedAt(now);
        plan.setStatus(CASE_CONTINUE.equals(outcome) ? PLAN_PUBLISHED : PLAN_REVIEWED);
        plan.setUpdatedAt(now);
        evidenceMapper.updateById(evidence);
        planMapper.updateById(plan);
        caseMapper.updateById(learningCase);
        return toCaseView(learningCase, plan, evidence, null, null);
    }

    @Override
    public LearningStudentGrowthVO getStudentGrowthOverview() {
        UserInfoDTO student = requireStudent();
        return buildStudentGrowthOverview(student, false);
    }

    @Override
    @Transactional
    public LearningStudentGrowthVO refreshStudentCourseRecommendations() {
        UserInfoDTO student = requireStudent();
        return buildStudentGrowthOverview(student, true);
    }

    @Override
    public LearningAssistantReplyVO askStudentAssistant(LearningAssistantRequest request) {
        UserInfoDTO student = requireStudent();
        LearningStudentGrowthVO overview = buildStudentGrowthOverview(student, false);
        String question = cleanText(request.getQuestion(), 800);
        String context = buildStudentAssistantContext(overview, request.getCourseId());
        Optional<CourseAssistantGateway.LearningQuestionResponse> response = courseAssistantGateway.answerLearningQuestion(
                new CourseAssistantGateway.LearningQuestionRequest("学生", context, question)
        );
        if (response.isPresent()) {
            CourseAssistantGateway.LearningQuestionResponse answer = response.get();
            return LearningAssistantReplyVO.builder().answer(answer.answer()).nextStep(answer.nextStep())
                    .recommendedChapter(answer.recommendedChapter()).references(answer.references()).source("MODEL")
                    .llmUsed(true).build();
        }
        LearningRiskAlertVO priority = overview.getRiskAlerts().isEmpty() ? null : overview.getRiskAlerts().getFirst();
        return LearningAssistantReplyVO.builder()
                .answer(priority == null
                        ? "从当前真实记录看，没有需要立即处理的学习风险。可以继续保持现有节奏，并优先完成正在学习的下一章节。"
                        : "从当前记录看，最需要先处理的是“" + priority.getCourseName() + "”：" + priority.getEvidence() + "。")
                .nextStep(priority == null ? "打开一门正在学习的课程，完成下一章节并留下新的学习记录。" : priority.getAction())
                .recommendedChapter(priority == null ? null : priority.getNextChapter())
                .references(priority == null ? List.of("真实课程学习记录", "当前能力画像")
                        : List.of(priority.getCourseName(), priority.getEvidence()))
                .source("FALLBACK").llmUsed(false).build();
    }

    @Override
    public LearningAssistantReplyVO askTeacherAssistant(Long classId, LearningAssistantRequest request) {
        LearningTeacherGrowthVO dashboard = getTeacherDashboard(classId);
        String question = cleanText(request.getQuestion(), 800);
        String context = buildTeacherAssistantContext(dashboard, request.getCourseId());
        Optional<CourseAssistantGateway.LearningQuestionResponse> response = courseAssistantGateway.answerLearningQuestion(
                new CourseAssistantGateway.LearningQuestionRequest("教师", context, question)
        );
        if (response.isPresent()) {
            CourseAssistantGateway.LearningQuestionResponse answer = response.get();
            return LearningAssistantReplyVO.builder().answer(answer.answer()).nextStep(answer.nextStep())
                    .recommendedChapter(answer.recommendedChapter()).references(answer.references()).source("MODEL")
                    .llmUsed(true).build();
        }
        LearningRiskAlertVO priority = dashboard.getRiskAlerts().isEmpty() ? null : dashboard.getRiskAlerts().getFirst();
        return LearningAssistantReplyVO.builder()
                .answer(priority == null
                        ? "本班当前没有中高风险的课程任务。建议继续观察趋势变化和学生实践类课程参与度。"
                        : priority.getStudentName() + "在“" + priority.getCourseName() + "”需要优先关注：" + priority.getEvidence() + "。")
                .nextStep(priority == null ? "结合近 7 天趋势，在下一次课前查看班级能力分布。" : priority.getAction())
                .recommendedChapter(priority == null ? null : priority.getNextChapter())
                .references(priority == null ? List.of("班级真实学习趋势", "班级能力画像")
                        : List.of(priority.getStudentName() + " · " + priority.getCourseName(), priority.getEvidence()))
                .source("FALLBACK").llmUsed(false).build();
    }

    private LearningStudentGrowthVO buildStudentGrowthOverview(UserInfoDTO student, boolean refreshRecommendations) {
        List<CourseContext> contexts = resolveStudentCourses(student.getUserId());
        List<LearningStudentOverviewVO.CourseLearning> courses = contexts.stream()
                .map(context -> toStudentCourse(student.getUserId(), context))
                .sorted(Comparator.comparing(LearningStudentOverviewVO.CourseLearning::getRiskScore).reversed())
                .toList();
        LearningCourseProfileVO profile = buildCourseProfileFromCourses(courses);
        List<LearningCourseRecommendationVO> recommendations = refreshRecommendations
                ? refreshRecommendations(student, courses, profile)
                : latestRecommendations(student.getUserId(), courses);
        if (recommendations.isEmpty()) {
            recommendations = fallbackRecommendations(student, courses, profile);
        }
        List<LearningGrowthCaseVO> cases = caseMapper.selectList(new LambdaQueryWrapper<LearningCasePO>()
                        .eq(LearningCasePO::getStudentId, student.getUserId())
                        .orderByDesc(LearningCasePO::getUpdatedAt)
                        .orderByDesc(LearningCasePO::getId))
                .stream()
                .filter(item -> !Set.of(CASE_DRAFT, CASE_REJECTED).contains(item.getStatus()))
                .map(this::toCaseView)
                .toList();
        LearningGrowthCaseVO priority = cases.stream()
                .filter(item -> Set.of(CASE_PUBLISHED, CASE_CONTINUE, CASE_EVIDENCE_SUBMITTED).contains(item.getStatus()))
                .findFirst().orElse(null);
        List<CourseSnapshot> snapshots = contexts.stream().map(context -> buildSnapshot(
                student.getUserId(), userRepository.selectUserById(student.getUserId()), context.clazz(), context.course(),
                context.assignment(), context.chapters(), studyRecordRepository.selectRecordsByStudentId(student.getUserId()).stream()
                        .filter(record -> Objects.equals(record.getCourseId(), context.course().getId())).toList()
        )).toList();
        return LearningStudentGrowthVO.builder()
                .modelName("能力画像和风险预警来自真实学习记录；大模型只基于这些已知证据提供建议")
                .summary(LearningStudentGrowthVO.Summary.builder()
                        .courseCount(courses.size())
                        .averageProgress(courses.isEmpty() ? 0 : (int) Math.round(courses.stream().mapToInt(LearningStudentOverviewVO.CourseLearning::getProgress).average().orElse(0)))
                        .studyMinutes(courses.stream().mapToInt(LearningStudentOverviewVO.CourseLearning::getStudyMinutes).sum())
                        .activePlans((int) cases.stream().filter(item -> Set.of(CASE_PUBLISHED, CASE_CONTINUE, CASE_EVIDENCE_SUBMITTED).contains(item.getStatus())).count())
                        .completedCycles((int) cases.stream().filter(item -> CASE_EFFECTIVE.equals(item.getStatus())).count())
                        .build())
                .courses(courses)
                .learningProfile(profile)
                .abilityProfile(buildAbilityProfile(snapshots))
                .riskAlerts(courses.stream().filter(item -> !"LOW".equals(item.getRiskLevel())).map(this::toRiskAlert).toList())
                .recommendations(recommendations)
                .priorityCase(priority)
                .cases(cases)
                .build();
    }

    private ClassAnalysis analyseClass(EduClassPO clazz) {
        List<EduClassStudentPO> students = classStudentRepository.selectStudentsByClassId(clazz.getId());
        Map<Long, SysUserPO> users = students.stream().map(EduClassStudentPO::getStudentId).distinct()
                .map(userRepository::selectUserById).filter(Objects::nonNull)
                .collect(Collectors.toMap(SysUserPO::getId, Function.identity()));
        List<LearningTeacherOverviewVO.StudentRisk> risks = new ArrayList<>();
        Map<Long, List<CourseSnapshot>> snapshotsByStudent = new LinkedHashMap<>();
        int courseCount = 0;
        for (EduCourseClassPO assignment : courseClassRepository.selectByClassId(clazz.getId())) {
            EduCoursePO course = courseRepository.selectCourseById(assignment.getCourseId());
            if (!isActiveCourse(course)) {
                continue;
            }
            courseCount++;
            List<EduChapterPO> chapters = activeChapters(course.getId());
            List<CourseSnapshot> snapshots = students.stream().map(relation -> buildSnapshot(
                    relation.getStudentId(), users.get(relation.getStudentId()), clazz, course, assignment, chapters,
                    studyRecordRepository.selectRecordsByStudentId(relation.getStudentId()).stream()
                            .filter(record -> Objects.equals(record.getCourseId(), course.getId())).toList()
            )).toList();
            int average = averageProgress(snapshots);
            snapshots.forEach(snapshot -> {
                risks.add(toTeacherRisk(snapshot, average));
                snapshotsByStudent.computeIfAbsent(snapshot.studentId(), ignored -> new ArrayList<>()).add(snapshot);
            });
        }
        risks.sort(Comparator.comparing(LearningTeacherOverviewVO.StudentRisk::getRiskScore).reversed()
                .thenComparing(LearningTeacherOverviewVO.StudentRisk::getStudentName, Comparator.nullsLast(String::compareTo)));
        int progress = risks.isEmpty() ? 0 : (int) Math.round(risks.stream().mapToInt(LearningTeacherOverviewVO.StudentRisk::getProgress).average().orElse(0));
        int active = (int) risks.stream().filter(item -> item.getIdleDays() != null && item.getIdleDays() <= 7)
                .map(LearningTeacherOverviewVO.StudentRisk::getStudentId).distinct().count();
        List<LearningStudentTypeProfileVO> studentProfiles = students.stream()
                .map(EduClassStudentPO::getStudentId)
                .distinct()
                .map(studentId -> LearningStudentTypeProfileVO.builder()
                        .studentId(studentId)
                        .studentName(defaultString(users.get(studentId) == null ? null : users.get(studentId).getRealName(), "未命名学生"))
                        .profile(buildCourseProfileFromSnapshots(snapshotsByStudent.getOrDefault(studentId, List.of())))
                        .build())
                .sorted(Comparator.comparing(LearningStudentTypeProfileVO::getStudentName))
                .toList();
        LearningCourseProfileVO classProfile = buildCourseProfileFromSnapshots(
                snapshotsByStudent.values().stream().flatMap(Collection::stream).toList()
        );
        return new ClassAnalysis(students, courseCount, progress, active, risks, classProfile, studentProfiles, snapshotsByStudent, users);
    }

    private CourseSnapshot requireSnapshot(EduClassPO clazz, Long courseId, Long studentId) {
        if (classStudentRepository.selectClassStudent(clazz.getId(), studentId) == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "该学生不在当前班级中");
        }
        EduCourseClassPO assignment = courseClassRepository.selectCourseClass(courseId, clazz.getId());
        if (assignment == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "该课程没有下发给当前班级");
        }
        EduCoursePO course = courseRepository.selectCourseById(courseId);
        if (!isActiveCourse(course)) {
            throw new BaseException(HttpStatus.NOT_FOUND, "课程不存在或已停止使用");
        }
        return buildSnapshot(studentId, userRepository.selectUserById(studentId), clazz, course, assignment,
                activeChapters(courseId), studyRecordRepository.selectRecordsByStudentId(studentId).stream()
                        .filter(record -> Objects.equals(record.getCourseId(), courseId)).toList());
    }

    private CourseSnapshot requireSnapshot(LearningCasePO learningCase) {
        EduClassPO clazz = classRepository.selectClassById(learningCase.getClassId());
        if (clazz == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "学习案例所属班级不存在");
        }
        return requireSnapshot(clazz, learningCase.getCourseId(), learningCase.getStudentId());
    }

    private CourseSnapshot buildSnapshot(
            Long studentId,
            SysUserPO student,
            EduClassPO clazz,
            EduCoursePO course,
            EduCourseClassPO assignment,
            List<EduChapterPO> chapters,
            List<EduStudyRecordPO> records
    ) {
        Map<Long, EduStudyRecordPO> latestByChapter = new HashMap<>();
        for (EduStudyRecordPO record : records) {
            if (record.getChapterId() != null) {
                latestByChapter.merge(record.getChapterId(), record, this::newerRecord);
            }
        }
        Long assignmentId = assignment == null ? 0L : assignment.getId();
        for (ChapterResourceProgressVO summary : resourceProgressService.summarizeChapters(studentId, course.getId(), assignmentId)) {
            if (!Boolean.TRUE.equals(summary.getHasResourceRecords())) continue;
            EduStudyRecordPO previous = latestByChapter.get(summary.getChapterId());
            latestByChapter.put(summary.getChapterId(), EduStudyRecordPO.builder()
                    .id(previous == null ? null : previous.getId()).studentId(studentId).courseId(course.getId())
                    .chapterId(summary.getChapterId()).progress(summary.getProgress()).finishStatus(summary.getFinishStatus())
                    .studyDuration(previous == null ? 0 : defaultNumber(previous.getStudyDuration()))
                    .lastStudyTime(previous == null ? null : previous.getLastStudyTime()).build());
        }
        int total = chapters.size();
        int finished = 0;
        int progressSum = 0;
        int minutes = 0;
        LocalDateTime last = null;
        EduChapterPO next = null;
        for (EduChapterPO chapter : chapters) {
            EduStudyRecordPO record = latestByChapter.get(chapter.getId());
            int chapterProgress = normalizeProgress(record == null ? null : record.getProgress());
            progressSum += chapterProgress;
            if (chapterProgress >= 100 || (record != null && Integer.valueOf(1).equals(record.getFinishStatus()))) {
                finished++;
            } else if (next == null) {
                next = chapter;
            }
            if (record != null) {
                minutes += defaultNumber(record.getStudyDuration());
                if (record.getLastStudyTime() != null && (last == null || record.getLastStudyTime().isAfter(last))) {
                    last = record.getLastStudyTime();
                }
            }
        }
        int progress = total == 0 ? 0 : (int) Math.round(progressSum / (double) total);
        if (next == null && !chapters.isEmpty() && progress < 100) {
            next = chapters.getFirst();
        }
        return new CourseSnapshot(studentId, student, clazz, course, assignment, chapters, progress, finished, minutes,
                last, daysSince(last), next == null ? null : next.getId(), next == null ? "课程复盘" : next.getChapterName());
    }

    private LearningTeacherOverviewVO.StudentRisk toTeacherRisk(CourseSnapshot snapshot, int classAverage) {
        LearningRiskModel.LearningRiskResult risk = assess(snapshot, classAverage);
        return LearningTeacherOverviewVO.StudentRisk.builder()
                .studentId(snapshot.studentId())
                .studentName(defaultString(snapshot.student() == null ? null : snapshot.student().getRealName(), "未命名学生"))
                .courseId(snapshot.course().getId())
                .courseName(snapshot.course().getCourseName())
                .progress(snapshot.progress())
                .courseAverage(classAverage)
                .totalChapters(snapshot.chapters().size())
                .finishedChapters(snapshot.finishedChapters())
                .studyMinutes(snapshot.studyMinutes())
                .lastStudyTime(formatDateTime(snapshot.lastStudyTime()))
                .idleDays(snapshot.idleDays())
                .deadline(formatDateTime(snapshot.assignment().getDeadline()))
                .deadlineDays(daysUntil(snapshot.assignment().getDeadline()))
                .riskScore(risk.score())
                .riskLevel(risk.level())
                .estimatedDays(risk.estimatedDays())
                .nextChapter(snapshot.nextChapter())
                .recommendation(risk.recommendation())
                .factors(toRiskFactors(risk.factors()))
                .build();
    }

    private LearningStudentOverviewVO.CourseLearning toStudentCourse(Long studentId, CourseContext context) {
        CourseSnapshot snapshot = buildSnapshot(studentId, userRepository.selectUserById(studentId), context.clazz(),
                context.course(), context.assignment(), context.chapters(), studyRecordRepository.selectRecordsByStudentId(studentId)
                        .stream().filter(record -> Objects.equals(record.getCourseId(), context.course().getId())).toList());
        int average = classCourseAverage(context.clazz().getId(), context.course(), context.assignment(), context.chapters());
        LearningRiskModel.LearningRiskResult risk = assess(snapshot, average);
        return LearningStudentOverviewVO.CourseLearning.builder()
                .classId(context.clazz().getId()).className(context.clazz().getClassName())
                .courseId(context.course().getId()).courseName(context.course().getCourseName())
                .courseType(normalizeCourseType(context.course().getCourseType())).courseTypeName(courseTypeName(context.course().getCourseType()))
                .courseCategory(courseCategory(context.course()))
                .difficulty(defaultNumber(context.course().getDifficulty()))
                .progress(snapshot.progress()).totalChapters(snapshot.chapters().size()).finishedChapters(snapshot.finishedChapters())
                .studyMinutes(snapshot.studyMinutes()).lastStudyTime(formatDateTime(snapshot.lastStudyTime()))
                .idleDays(snapshot.idleDays()).deadline(formatDateTime(context.assignment().getDeadline()))
                .deadlineDays(daysUntil(context.assignment().getDeadline())).riskScore(risk.score()).riskLevel(risk.level())
                .estimatedDays(risk.estimatedDays()).nextChapter(snapshot.nextChapter()).recommendation(risk.recommendation())
                .factors(toRiskFactors(risk.factors())).build();
    }

    private LearningAbilityProfileVO buildAbilityProfile(List<CourseSnapshot> snapshots) {
        List<CourseSnapshot> studied = snapshots.stream()
                .filter(item -> item.studyMinutes() > 0 || item.progress() > 0).toList();
        int courseCount = studied.size();
        int totalMinutes = studied.stream().mapToInt(CourseSnapshot::studyMinutes).sum();
        int progress = courseCount == 0 ? 0 : averageProgress(studied);
        int activeCourses = (int) studied.stream().filter(item -> item.idleDays() != null && item.idleDays() <= 7).count();
        int averageIdleDays = courseCount == 0 ? 0 : (int) Math.round(studied.stream().map(CourseSnapshot::idleDays)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(14));
        int investment = courseCount == 0 ? 0 : Math.min(100,
                (int) Math.round(totalMinutes * 100.0 / Math.max(45, courseCount * 45)));
        int activeRatio = courseCount == 0 ? 0 : (int) Math.round(activeCourses * 100.0 / courseCount);
        int continuity = courseCount == 0 ? 0 : clampScore((int) Math.round(activeRatio * 0.65
                + Math.max(0, 100 - averageIdleDays * 8) * 0.35));
        int overall = courseCount == 0 ? 0 : clampScore((int) Math.round(progress * 0.50 + investment * 0.25 + continuity * 0.25));
        int confidence = courseCount == 0 ? 0 : Math.min(95, 30 + courseCount * 12 + Math.min(29, totalMinutes / 8));

        List<LearningAbilityProfileVO.Dimension> dimensions = List.of(
                LearningAbilityProfileVO.Dimension.builder().key("progress").label("课程推进")
                        .score(progress).evidence(courseCount == 0 ? "尚无章节学习记录" : "已学习 " + courseCount + " 门课程，平均章节完成度 " + progress + "%")
                        .interpretation(scoreMeaning(progress, "课程推进较顺畅", "需要补齐未完成章节")).build(),
                LearningAbilityProfileVO.Dimension.builder().key("investment").label("学习投入")
                        .score(investment).evidence("累计 " + totalMinutes + " 分钟，平均每门课程 " + (courseCount == 0 ? 0 : Math.round(totalMinutes * 1.0 / courseCount)) + " 分钟")
                        .interpretation(scoreMeaning(investment, "学习投入较充足", "建议增加一次完整学习时段")).build(),
                LearningAbilityProfileVO.Dimension.builder().key("continuity").label("学习连续性")
                        .score(continuity).evidence("近 7 天活跃课程 " + activeCourses + "/" + courseCount + " 门；平均中断 " + averageIdleDays + " 天")
                        .interpretation(scoreMeaning(continuity, "学习节奏较稳定", "需要恢复固定学习节奏")).build()
        );
        LearningAbilityProfileVO.Dimension dominantDimension = dimensions.stream()
                .max(Comparator.comparing(LearningAbilityProfileVO.Dimension::getScore)).orElse(dimensions.getFirst());
        LearningAbilityProfileVO.Dimension priorityDimension = dimensions.stream()
                .min(Comparator.comparing(LearningAbilityProfileVO.Dimension::getScore)).orElse(dimensions.getFirst());
        int balanceScore = courseCount == 0 ? 0 : clampScore(100 - (dominantDimension.getScore() - priorityDimension.getScore()));
        String pattern = courseCount == 0 ? "数据积累中"
                : overall < 50 ? "基础待巩固"
                : balanceScore >= 88 ? "均衡发展"
                : dominantDimension.getLabel() + "突出";
        List<String> strengths = dimensions.stream().filter(item -> item.getScore() >= 70)
                .map(item -> item.getLabel() + "表现较稳：" + item.getEvidence()).toList();
        List<String> gaps = dimensions.stream().filter(item -> item.getScore() < 60)
                .map(item -> item.getLabel() + "需要关注：" + item.getEvidence()).toList();
        List<String> actions = new ArrayList<>();
        if (progress < 60) actions.add("优先完成当前课程的下一未完成章节，再查看课程推进变化。");
        if (investment < 60) actions.add("本周安排一次不少于 30 分钟的完整学习，补足有效学习时长。");
        if (continuity < 60) actions.add("在本周安排 2 次学习，减少课程学习中断。");
        if (actions.isEmpty()) actions.add("保持当前学习节奏，完成下一章节后再查看画像变化。");
        String summary = courseCount == 0 ? "暂无足够学习记录，完成课程章节后会形成能力画像。"
                : "画像基于 " + courseCount + " 个课程学习样本、" + totalMinutes + " 分钟真实时长计算，反映课程推进、学习投入和学习连续性。";
        return LearningAbilityProfileVO.builder().overallScore(overall).level(abilityLevel(overall)).dataConfidence(confidence)
                .summary(summary)
                .pattern(pattern)
                .balanceScore(balanceScore)
                .dominantDimensionKey(dominantDimension.getKey())
                .priorityDimensionKey(priorityDimension.getKey())
                .dimensions(dimensions).strengths(strengths).gaps(gaps).nextActions(actions).build();
    }

    private List<LearningStudentAbilityVO> buildStudentAbilities(ClassAnalysis analysis) {
        return analysis.snapshotsByStudent().entrySet().stream().map(entry -> {
            Long studentId = entry.getKey();
            LearningTeacherOverviewVO.StudentRisk topRisk = analysis.risks().stream()
                    .filter(item -> Objects.equals(item.getStudentId(), studentId)).findFirst().orElse(null);
            SysUserPO student = analysis.users().get(studentId);
            LearningAbilityProfileVO profile = buildAbilityProfile(entry.getValue());
            StudentLearningState state = studentLearningState(profile, topRisk);
            return LearningStudentAbilityVO.builder().studentId(studentId)
                    .studentName(defaultString(student == null ? null : student.getRealName(), "未命名学生"))
                    .abilityProfile(profile)
                    .learningState(state.code())
                    .learningStateLabel(state.label())
                    .priorityReason(state.reason())
                    .recommendedAction(state.action())
                    .topRiskScore(topRisk == null ? 0 : topRisk.getRiskScore())
                    .topRiskLevel(topRisk == null ? "LOW" : topRisk.getRiskLevel())
                    .topRiskCourse(topRisk == null ? "暂无风险课程" : topRisk.getCourseName()).build();
        }).sorted(Comparator.comparing(item -> item.getAbilityProfile().getOverallScore())).toList();
    }

    private StudentLearningState studentLearningState(
            LearningAbilityProfileVO profile,
            LearningTeacherOverviewVO.StudentRisk topRisk
    ) {
        LearningAbilityProfileVO.Dimension gap = profile.getDimensions().stream()
                .min(Comparator.comparing(LearningAbilityProfileVO.Dimension::getScore)).orElse(null);
        String action = topRisk == null ? profile.getNextActions().getFirst() : topRisk.getRecommendation();
        if (topRisk != null && "HIGH".equals(topRisk.getRiskLevel()) || profile.getOverallScore() < 50) {
            String reason = topRisk == null
                    ? defaultString(gap == null ? null : gap.getLabel(), "学习记录") + "偏低，需要优先介入"
                    : topRisk.getCourseName() + "风险 " + topRisk.getRiskScore() + "：" + defaultString(topRisk.getFactors().isEmpty() ? null : topRisk.getFactors().getFirst().getEvidence(), "需要优先跟进");
            return new StudentLearningState("FOCUS", "重点干预", reason, action);
        }
        if (topRisk != null && "MEDIUM".equals(topRisk.getRiskLevel()) || profile.getOverallScore() < 70
                || (gap != null && gap.getScore() < 60)) {
            String reason = topRisk == null
                    ? defaultString(gap == null ? null : gap.getLabel(), "学习推进") + "需要关注：" + defaultString(gap == null ? null : gap.getEvidence(), "请查看学习记录")
                    : topRisk.getCourseName() + "存在中等风险：" + defaultString(topRisk.getFactors().isEmpty() ? null : topRisk.getFactors().getFirst().getEvidence(), "请及时跟进");
            return new StudentLearningState("ATTENTION", "需要关注", reason, action);
        }
        String reason = "课程推进和学习节奏保持稳定，可继续完成下一章节。";
        return new StudentLearningState("STEADY", "稳定推进", reason, action);
    }

    private LearningRiskAlertVO toRiskAlert(LearningStudentOverviewVO.CourseLearning course) {
        String evidence = course.getFactors().stream().limit(2).map(LearningTeacherOverviewVO.RiskFactor::getEvidence)
                .collect(Collectors.joining("；"));
        return LearningRiskAlertVO.builder().courseId(course.getCourseId()).courseName(course.getCourseName())
                .riskScore(course.getRiskScore()).riskLevel(course.getRiskLevel()).title(riskTitle(course.getRiskLevel(), course.getCourseName()))
                .evidence(evidence).action(course.getRecommendation()).nextChapter(course.getNextChapter()).build();
    }

    private LearningRiskAlertVO toRiskAlert(LearningTeacherOverviewVO.StudentRisk risk) {
        String evidence = risk.getFactors().stream().limit(2).map(LearningTeacherOverviewVO.RiskFactor::getEvidence)
                .collect(Collectors.joining("；"));
        return LearningRiskAlertVO.builder().studentId(risk.getStudentId()).studentName(risk.getStudentName())
                .courseId(risk.getCourseId()).courseName(risk.getCourseName()).riskScore(risk.getRiskScore())
                .riskLevel(risk.getRiskLevel()).title(risk.getStudentName() + "：" + riskTitle(risk.getRiskLevel(), risk.getCourseName()))
                .evidence(evidence).action(risk.getRecommendation()).nextChapter(risk.getNextChapter()).build();
    }

    private LearningClassTrendVO buildClassTrend(List<EduClassStudentPO> students) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        Map<LocalDate, Integer> minutesByDate = new TreeMap<>();
        Map<LocalDate, Set<Long>> studentsByDate = new TreeMap<>();
        for (EduClassStudentPO relation : students) {
            for (EduStudyRecordPO record : studyRecordRepository.selectRecordsByStudentId(relation.getStudentId())) {
                if (record.getLastStudyTime() == null) continue;
                LocalDate date = record.getLastStudyTime().toLocalDate();
                if (date.isBefore(start) || date.isAfter(today)) continue;
                minutesByDate.merge(date, defaultNumber(record.getStudyDuration()), Integer::sum);
                studentsByDate.computeIfAbsent(date, ignored -> new java.util.HashSet<>()).add(relation.getStudentId());
            }
        }
        List<LearningClassTrendVO.DailyTrend> days = new ArrayList<>();
        int totalMinutes = 0;
        Set<Long> activeStudents = new java.util.HashSet<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            int minutes = minutesByDate.getOrDefault(date, 0);
            Set<Long> active = studentsByDate.getOrDefault(date, Set.of());
            totalMinutes += minutes;
            activeStudents.addAll(active);
            days.add(LearningClassTrendVO.DailyTrend.builder().date(date.getMonthValue() + "/" + date.getDayOfMonth())
                    .studyMinutes(minutes).activeStudents(active.size()).build());
        }
        return LearningClassTrendVO.builder().period("近 7 天").totalStudyMinutes(totalMinutes)
                .activeStudents(activeStudents.size()).days(days).build();
    }

    private String buildStudentAssistantContext(LearningStudentGrowthVO overview, Long courseId) {
        LearningAbilityProfileVO profile = overview.getAbilityProfile();
        String dimensions = profile.getDimensions().stream()
                .map(item -> item.getLabel() + " " + item.getScore() + "分（" + item.getEvidence() + "）")
                .collect(Collectors.joining("；"));
        String themes = overview.getLearningProfile().getShares().stream()
                .map(item -> item.getTypeName() + " " + item.getShare() + "%（" + item.getStudyMinutes() + "分钟）")
                .collect(Collectors.joining("；"));
        String courses = overview.getCourses().stream().filter(item -> courseId == null || Objects.equals(item.getCourseId(), courseId))
                .limit(4).map(item -> item.getCourseName() + "【" + item.getCourseCategory() + "】：完成 " + item.getProgress()
                        + "%；" + item.getStudyMinutes() + " 分钟；停学 " + defaultNumber(item.getIdleDays()) + " 天；下一章 " + item.getNextChapter())
                .collect(Collectors.joining("；"));
        String alerts = overview.getRiskAlerts().stream().limit(3).map(item -> item.getCourseName() + "：" + item.getEvidence())
                .collect(Collectors.joining("；"));
        return "能力画像：总分 " + profile.getOverallScore() + "，" + profile.getSummary()
                + "\n能力维度：" + defaultString(dimensions, "暂无")
                + "\n课程主题投入：" + defaultString(themes, "暂无")
                + "\n课程事实：" + defaultString(courses, "暂无")
                + "\n需要关注：" + defaultString(alerts, "暂无中高风险");
    }

    private String buildTeacherAssistantContext(LearningTeacherGrowthVO dashboard, Long courseId) {
        LearningAbilityProfileVO profile = dashboard.getClassAbilityProfile();
        String dimensions = profile.getDimensions().stream()
                .map(item -> item.getLabel() + " " + item.getScore() + "分（" + item.getEvidence() + "）")
                .collect(Collectors.joining("；"));
        String themes = dashboard.getClassProfile().getShares().stream()
                .map(item -> item.getTypeName() + " " + item.getShare() + "%（" + item.getStudyMinutes() + "分钟）")
                .collect(Collectors.joining("；"));
        String alerts = dashboard.getRiskAlerts().stream().filter(item -> courseId == null || Objects.equals(item.getCourseId(), courseId))
                .limit(4).map(item -> item.getStudentName() + "-" + item.getCourseName() + "：" + item.getEvidence())
                .collect(Collectors.joining("；"));
        LearningClassTrendVO trend = dashboard.getClassTrend();
        String dailyTrend = trend.getDays().stream()
                .map(item -> item.getDate() + " " + item.getStudyMinutes() + "分钟/" + item.getActiveStudents() + "人")
                .collect(Collectors.joining("；"));
        return "班级能力画像：总分 " + profile.getOverallScore() + "，" + profile.getSummary()
                + "\n能力维度：" + defaultString(dimensions, "暂无")
                + "\n课程主题投入：" + defaultString(themes, "暂无")
                + "\n近 7 天真实学习：" + trend.getTotalStudyMinutes() + " 分钟，" + trend.getActiveStudents() + " 名活跃学生"
                + "\n每日趋势：" + defaultString(dailyTrend, "暂无")
                + "\n具体预警：" + defaultString(alerts, "暂无中高风险");
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String abilityLevel(int score) {
        return score >= 80 ? "能力稳固" : score >= 60 ? "稳步提升" : score >= 40 ? "需要巩固" : "起步阶段";
    }

    private String scoreMeaning(int score, String positive, String improvement) {
        return score >= 70 ? positive : improvement;
    }

    private String riskTitle(String level, String courseName) {
        return switch (level) {
            case "HIGH" -> courseName + "存在明确学习断点，建议优先处理";
            case "MEDIUM" -> courseName + "的学习节奏需要跟进";
            default -> courseName + "当前节奏稳定";
        };
    }

    private LearningCourseProfileVO buildCourseProfileFromCourses(List<LearningStudentOverviewVO.CourseLearning> courses) {
        Map<String, Integer> minutesByType = new HashMap<>();
        Map<String, Set<Long>> coursesByType = new HashMap<>();
        for (LearningStudentOverviewVO.CourseLearning course : courses) {
            String type = defaultString(course.getCourseCategory(), course.getCourseName());
            minutesByType.merge(type, defaultNumber(course.getStudyMinutes()), Integer::sum);
            coursesByType.computeIfAbsent(type, ignored -> new java.util.HashSet<>()).add(course.getCourseId());
        }
        return buildCourseProfile(minutesByType, coursesByType);
    }

    private LearningCourseProfileVO buildCourseProfileFromSnapshots(List<CourseSnapshot> snapshots) {
        Map<String, Integer> minutesByType = new HashMap<>();
        Map<String, Set<Long>> coursesByType = new HashMap<>();
        for (CourseSnapshot snapshot : snapshots) {
            String type = courseCategory(snapshot.course());
            minutesByType.merge(type, snapshot.studyMinutes(), Integer::sum);
            coursesByType.computeIfAbsent(type, ignored -> new java.util.HashSet<>()).add(snapshot.course().getId());
        }
        return buildCourseProfile(minutesByType, coursesByType);
    }

    private LearningCourseProfileVO buildCourseProfile(
            Map<String, Integer> minutesByType,
            Map<String, Set<Long>> coursesByType
    ) {
        int total = minutesByType.values().stream().mapToInt(Integer::intValue).sum();
        int usedShare = 0;
        List<LearningCourseProfileVO.TypeShare> shares = new ArrayList<>();
        List<String> types = minutesByType.keySet().stream()
                .sorted(Comparator.comparing((String type) -> minutesByType.getOrDefault(type, 0)).reversed().thenComparing(String::compareTo))
                .toList();
        for (int index = 0; index < types.size(); index++) {
            String type = types.get(index);
            int minutes = minutesByType.getOrDefault(type, 0);
            int share = total == 0 ? 0 : (index == types.size() - 1
                    ? Math.max(0, 100 - usedShare) : (int) Math.round(minutes * 100.0 / total));
            usedShare += share;
            shares.add(LearningCourseProfileVO.TypeShare.builder()
                    .categoryKey(type).typeName(type).studyMinutes(minutes)
                    .courseCount(coursesByType.getOrDefault(type, Set.of()).size()).share(share).build());
        }
        LearningCourseProfileVO.TypeShare dominant = shares.stream()
                .max(Comparator.comparing(LearningCourseProfileVO.TypeShare::getStudyMinutes))
                .orElse(null);
        String dominantType = total == 0 ? "暂无学习记录" : dominant.getTypeName();
        String insight = total == 0
                ? "尚未形成可分析的观看记录，完成课程学习后会更新画像。"
                : "最近学习时长主要集中在" + dominantType + "（" + dominant.getShare() + "%），累计 " + total + " 分钟。";
        return LearningCourseProfileVO.builder()
                .totalStudyMinutes(total).dominantType(dominantType).insight(insight).shares(shares).build();
    }

    private List<LearningCourseRecommendationVO> latestRecommendations(
            Long studentId,
            List<LearningStudentOverviewVO.CourseLearning> courses
    ) {
        List<LearningRecommendationPO> rows = recommendationMapper.selectList(new LambdaQueryWrapper<LearningRecommendationPO>()
                .eq(LearningRecommendationPO::getStudentId, studentId)
                .orderByDesc(LearningRecommendationPO::getCreatedAt)
                .orderByDesc(LearningRecommendationPO::getId));
        if (rows.isEmpty()) {
            return List.of();
        }
        String batchId = rows.getFirst().getBatchId();
        return toPersistedRecommendationViews(rows.stream().filter(row -> Objects.equals(batchId, row.getBatchId())).limit(3).toList());
    }

    private List<LearningCourseRecommendationVO> refreshRecommendations(
            UserInfoDTO student,
            List<LearningStudentOverviewVO.CourseLearning> courses,
            LearningCourseProfileVO profile
    ) {
        List<RecommendationCandidate> candidates = recommendationCandidates(student.getUserId(), courses);
        if (candidates.isEmpty()) {
            return List.of();
        }
        long started = System.currentTimeMillis();
        Optional<List<CourseAssistantGateway.CourseRecommendationResponse>> modelRecommendations = courseAssistantGateway.recommendCourses(
                new CourseAssistantGateway.RecommendationRequest(
                        buildProfileSummary(profile, courses),
                        candidates.stream().map(item -> new CourseAssistantGateway.CourseCandidate(
                                item.course().getId(), item.course().getCourseName(), courseCategory(item.course()),
                                item.course().getDifficulty(), defaultString(item.course().getIntro(), "未提供课程简介")
                        )).toList()
                )
        );
        long elapsed = System.currentTimeMillis() - started;
        Map<Long, RecommendationCandidate> candidateById = candidates.stream()
                .collect(Collectors.toMap(item -> item.course().getId(), Function.identity()));
        List<RecommendationDraft> drafts = modelRecommendations.map(items -> items.stream()
                        .filter(item -> candidateById.containsKey(item.courseId()))
                        .map(item -> new RecommendationDraft(candidateById.get(item.courseId()).course(), item.score(), item.reason(), "MODEL", item.modelName()))
                        .limit(3).toList())
                .filter(items -> !items.isEmpty())
                .orElseGet(() -> fallbackRecommendationDrafts(candidates, profile));
        String batchId = "rec-" + student.getUserId() + "-" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        for (RecommendationDraft draft : drafts) {
            recommendationMapper.insert(LearningRecommendationPO.builder()
                    .batchId(batchId).studentId(student.getUserId()).courseId(draft.course().getId())
                    .recommendationScore(draft.score()).reason(draft.reason()).source(draft.source())
                    .modelName(draft.modelName()).createdAt(now).build());
        }
        RecommendationDraft first = drafts.getFirst();
        saveTrace(null, null, student.getUserId(), "COURSE_RECOMMENDATION", first.modelName(), first.source(),
                abbreviate(buildProfileSummary(profile, courses), 1000), elapsed);
        return toRecommendationViews(drafts);
    }

    private List<LearningCourseRecommendationVO> fallbackRecommendations(
            UserInfoDTO student,
            List<LearningStudentOverviewVO.CourseLearning> courses,
            LearningCourseProfileVO profile
    ) {
        return toRecommendationViews(fallbackRecommendationDrafts(recommendationCandidates(student.getUserId(), courses), profile));
    }

    private List<RecommendationCandidate> recommendationCandidates(
            Long studentId,
            List<LearningStudentOverviewVO.CourseLearning> studiedCourses
    ) {
        Set<Long> studiedIds = studiedCourses.stream().map(LearningStudentOverviewVO.CourseLearning::getCourseId).collect(Collectors.toSet());
        SysUserPO student = userRepository.selectUserById(studentId);
        String grade = student == null ? null : student.getGrade();
        return courseRepository.selectPublicCourses().stream()
                .filter(this::isActiveCourse)
                .filter(course -> Integer.valueOf(1).equals(course.getPublicFlag()))
                .filter(course -> !studiedIds.contains(course.getId()))
                .filter(course -> !StringUtils.hasText(grade) || "通用".equals(course.getGrade()) || Objects.equals(grade, course.getGrade()))
                .map(RecommendationCandidate::new)
                .sorted(Comparator.comparing((RecommendationCandidate item) -> item.course().getDifficulty(), Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(item -> item.course().getId()))
                .limit(12)
                .toList();
    }

    private List<RecommendationDraft> fallbackRecommendationDrafts(
            List<RecommendationCandidate> candidates,
            LearningCourseProfileVO profile
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<String, LearningCourseProfileVO.TypeShare> shares = profile.getShares().stream()
                .collect(Collectors.toMap(LearningCourseProfileVO.TypeShare::getCategoryKey, Function.identity()));
        String dominantType = shares.values().stream().max(Comparator.comparing(LearningCourseProfileVO.TypeShare::getStudyMinutes))
                .map(LearningCourseProfileVO.TypeShare::getCategoryKey).orElse("");
        return candidates.stream().map(item -> {
                    EduCoursePO course = item.course();
                    String type = courseCategory(course);
                    int currentShare = shares.getOrDefault(type, LearningCourseProfileVO.TypeShare.builder().share(0).build()).getShare();
                    int score = 52 + (Objects.equals(type, dominantType) ? 22 : 0) + (currentShare == 0 ? 14 : 0)
                            + (defaultNumber(course.getDifficulty()) <= 2 ? 6 : 0);
                    String reason = Objects.equals(type, dominantType)
                            ? "你近期学习时长主要集中在“" + type + "”，这门课程可在同一主题上继续深入。"
                            : currentShare == 0
                            ? "你的学习记录中暂缺“" + type + "”主题，这门课程可补足当前学习主题结构。"
                            : "这门“" + type + "”课程可与当前学习主题形成互补，扩展学习路径。";
                    return new RecommendationDraft(course, Math.min(100, score), reason, "FALLBACK", "course-data-fallback");
                })
                .sorted(Comparator.comparing(RecommendationDraft::score).reversed().thenComparing(item -> item.course().getId()))
                .limit(3).toList();
    }

    private List<LearningCourseRecommendationVO> toPersistedRecommendationViews(List<LearningRecommendationPO> rows) {
        return rows.stream().map(row -> {
                    EduCoursePO course = courseRepository.selectCourseById(row.getCourseId());
                    if (!isActiveCourse(course) || !Integer.valueOf(1).equals(course.getPublicFlag())) {
                        return null;
                    }
                    return LearningCourseRecommendationVO.builder()
                            .courseId(course.getId()).courseName(course.getCourseName()).courseType(normalizeCourseType(course.getCourseType()))
                            .courseCategory(courseCategory(course))
                            .courseTypeName(courseTypeName(course.getCourseType())).difficulty(defaultNumber(course.getDifficulty()))
                            .intro(defaultString(course.getIntro(), "暂无课程简介")).score(defaultNumber(row.getRecommendationScore()))
                            .reason(row.getReason()).source(row.getSource()).modelName(row.getModelName()).build();
                })
                .filter(Objects::nonNull).toList();
    }

    private List<LearningCourseRecommendationVO> toRecommendationViews(List<RecommendationDraft> drafts) {
        return drafts.stream().map(draft -> LearningCourseRecommendationVO.builder()
                .courseId(draft.course().getId()).courseName(draft.course().getCourseName())
                .courseType(normalizeCourseType(draft.course().getCourseType())).courseTypeName(courseTypeName(draft.course().getCourseType()))
                .courseCategory(courseCategory(draft.course()))
                .difficulty(defaultNumber(draft.course().getDifficulty())).intro(defaultString(draft.course().getIntro(), "暂无课程简介"))
                .score(draft.score()).reason(draft.reason()).source(draft.source()).modelName(draft.modelName()).build()).toList();
    }

    private String buildProfileSummary(LearningCourseProfileVO profile, List<LearningStudentOverviewVO.CourseLearning> courses) {
        String shares = profile.getShares().stream()
                .map(item -> item.getTypeName() + item.getShare() + "%（" + item.getStudyMinutes() + "分钟）")
                .collect(Collectors.joining("；"));
        String learned = courses.stream().limit(6).map(LearningStudentOverviewVO.CourseLearning::getCourseName)
                .collect(Collectors.joining("、"));
        return "课程主题占比：" + shares + "。主要主题：" + profile.getDominantType()
                + "。已学习课程：" + defaultString(learned, "暂无") + "。";
    }

    /**
     * course_type is a teaching-form field. The learning dashboard must instead
     * group time by the persisted subject tag; older courses fall back to name.
     */
    private String courseCategory(EduCoursePO course) {
        String fallback = course == null ? "未分类课程" : defaultString(course.getCourseName(), "未分类课程");
        if (course == null || !StringUtils.hasText(course.getExtJson())) {
            return fallback;
        }
        try {
            JsonNode root = objectMapper.readTree(course.getExtJson());
            JsonNode category = root.path("learningCategory");
            if (category.isTextual() && StringUtils.hasText(category.asText())) {
                return category.asText().trim();
            }
            JsonNode tags = root.isArray() ? root : root.path("tags");
            if (tags.isArray()) {
                for (JsonNode tag : tags) {
                    if (tag.isTextual() && StringUtils.hasText(tag.asText())) {
                        return tag.asText().trim();
                    }
                }
            }
        } catch (Exception ignored) {
            // Optional category metadata must never hide a student's study record.
        }
        return fallback;
    }

    private int normalizeCourseType(Integer courseType) {
        return courseType == null || courseType < 1 || courseType > 3 ? 1 : courseType;
    }

    private String courseTypeName(Integer courseType) {
        return switch (normalizeCourseType(courseType)) {
            case 2 -> "项目实践课";
            case 3 -> "实验课";
            default -> "理论课";
        };
    }

    private List<CourseContext> resolveStudentCourses(Long studentId) {
        List<CourseContext> contexts = new ArrayList<>();
        for (EduClassStudentPO membership : classStudentRepository.selectClassesByStudentId(studentId)) {
            EduClassPO clazz = classRepository.selectClassById(membership.getClassId());
            if (clazz == null || Integer.valueOf(1).equals(clazz.getDeleted()) || !Integer.valueOf(1).equals(clazz.getStatus())) {
                continue;
            }
            for (EduCourseClassPO assignment : courseClassRepository.selectByClassId(clazz.getId())) {
                EduCoursePO course = courseRepository.selectCourseById(assignment.getCourseId());
                if (isActiveCourse(course)) {
                    contexts.add(new CourseContext(clazz, course, assignment, activeChapters(course.getId())));
                }
            }
        }
        return contexts;
    }

    private int classCourseAverage(Long classId, EduCoursePO course, EduCourseClassPO assignment, List<EduChapterPO> chapters) {
        List<EduClassStudentPO> students = classStudentRepository.selectStudentsByClassId(classId);
        if (students.isEmpty()) {
            return 0;
        }
        List<CourseSnapshot> snapshots = students.stream().map(item -> buildSnapshot(
                item.getStudentId(), userRepository.selectUserById(item.getStudentId()), classRepository.selectClassById(classId),
                course, assignment, chapters, studyRecordRepository.selectRecordsByStudentId(item.getStudentId()).stream()
                        .filter(record -> Objects.equals(record.getCourseId(), course.getId())).toList())).toList();
        return averageProgress(snapshots);
    }

    private LearningRiskModel.LearningRiskResult assess(CourseSnapshot snapshot, int average) {
        return riskModel.assess(new LearningRiskModel.LearningRiskInput(
                snapshot.progress(), average, snapshot.idleDays(), daysUntil(snapshot.assignment().getDeadline()),
                snapshot.chapters().size(), snapshot.finishedChapters(), snapshot.studyMinutes()
        ));
    }

    private LearningGrowthCaseVO toCaseView(LearningCasePO learningCase) {
        return toCaseView(learningCase, requireLatestPlan(learningCase.getId()), null, null, null);
    }

    private LearningGrowthCaseVO toCaseView(
            LearningCasePO learningCase,
            LearningPlanPO plan,
            LearningEvidencePO suppliedEvidence,
            CourseSnapshot suppliedSnapshot,
            Integer suppliedAverage
    ) {
        LearningEvidencePO evidence = suppliedEvidence == null ? latestEvidence(plan.getId()) : suppliedEvidence;
        CourseSnapshot snapshot = suppliedSnapshot;
        Integer average = suppliedAverage;
        try {
            if (snapshot == null) {
                snapshot = requireSnapshot(learningCase);
            }
            if (average == null) {
                average = classCourseAverage(learningCase.getClassId(), snapshot.course(), snapshot.assignment(), snapshot.chapters());
            }
        } catch (BaseException ignored) {
            // Historical cases remain readable even if a teacher archives a class later.
        }
        EduCoursePO course = snapshot == null ? courseRepository.selectCourseById(learningCase.getCourseId()) : snapshot.course();
        SysUserPO student = snapshot == null ? userRepository.selectUserById(learningCase.getStudentId()) : snapshot.student();
        EduChapterPO chapter = learningCase.getChapterId() == null ? null : chapterRepository.selectChapterById(learningCase.getChapterId());
        int progress = snapshot == null ? snapshotNumber(learningCase.getBehaviorSnapshot(), "progress") : snapshot.progress();
        int minutes = snapshot == null ? snapshotNumber(learningCase.getBehaviorSnapshot(), "studyMinutes") : snapshot.studyMinutes();
        int beforeProgress = snapshotNumber(learningCase.getBehaviorSnapshot(), "progress");
        int beforeMinutes = snapshotNumber(learningCase.getBehaviorSnapshot(), "studyMinutes");
        return LearningGrowthCaseVO.builder()
                .caseId(learningCase.getId()).classId(learningCase.getClassId()).courseId(learningCase.getCourseId())
                .chapterId(learningCase.getChapterId()).courseName(course == null ? "课程已归档" : course.getCourseName())
                .chapterName(chapter == null ? "课程复盘" : chapter.getChapterName())
                .studentId(learningCase.getStudentId()).studentName(defaultString(student == null ? null : student.getRealName(), "未命名学生"))
                .riskScore(learningCase.getRiskScore()).riskLevel(learningCase.getRiskLevel()).progress(progress)
                .courseAverage(average == null ? 0 : average).studyMinutes(minutes)
                .idleDays(snapshot == null ? null : snapshot.idleDays())
                .factors(snapshot == null ? List.of() : toRiskFactors(assess(snapshot, average == null ? 0 : average).factors()))
                .diagnosis(learningCase.getDiagnosis()).diagnosisSource(learningCase.getDiagnosisSource()).modelName(learningCase.getModelName())
                .status(learningCase.getStatus()).createdAt(formatDateTime(learningCase.getCreatedAt())).updatedAt(formatDateTime(learningCase.getUpdatedAt()))
                .planId(plan.getId()).planStatus(plan.getStatus()).teacherDecision(plan.getTeacherDecision()).title(plan.getTitle())
                .learningGoal(plan.getLearningGoal()).taskSteps(parseList(plan.getTaskSteps())).durationMinutes(plan.getDurationMinutes())
                .acceptanceCriteria(plan.getAcceptanceCriteria()).checkQuestion(plan.getCheckQuestion()).expectedSignals(parseList(plan.getExpectedSignals()))
                .evidenceId(evidence == null ? null : evidence.getId()).reflection(evidence == null ? null : evidence.getReflection())
                .difficulty(evidence == null ? null : evidence.getDifficulty()).answer(evidence == null ? null : evidence.getAnswer())
                .aiAssessment(evidence == null ? null : evidence.getAiAssessment()).confidence(evidence == null ? null : evidence.getConfidence())
                .evidenceResult(evidence == null ? null : evidence.getResult()).assessmentSource(evidence == null ? null : evidence.getAssessmentSource())
                .teacherConclusion(evidence == null ? null : evidence.getTeacherConclusion()).submittedAt(evidence == null ? null : formatDateTime(evidence.getSubmittedAt()))
                .reviewedAt(evidence == null ? null : formatDateTime(evidence.getReviewedAt()))
                .progressChange(progress - beforeProgress).studyMinutesChange(minutes - beforeMinutes)
                .improvementSummary(buildImprovementSummary(progress - beforeProgress, minutes - beforeMinutes, evidence))
                .build();
    }

    private PlanDraft fallbackPlan(CourseSnapshot snapshot, LearningRiskModel.LearningRiskResult risk) {
        String chapter = snapshot.nextChapter();
        String diagnosis = "学习记录显示“" + snapshot.course().getCourseName() + "”当前完成 " + snapshot.progress()
                + "%；" + risk.factors().getFirst().evidence() + "。建议先用一个短任务恢复连续学习，再检查是否真正理解。";
        return new PlanDraft(
                diagnosis,
                "在“" + chapter + "”完成一个可验证的小步骤，建立下一次学习的起点。",
                List.of("进入课程的“" + chapter + "”，连续学习 15 分钟并完成一个小节。", "用自己的话写下本节一个核心概念及其用途。", "根据理解检查问题作答；不确定的地方如实写入困难点。"),
                25,
                "学习记录产生新的章节进度或时长；提交自己的概念说明和理解检查回答。",
                "请用自己的话说明“" + chapter + "”中你刚学到的一个核心概念，并举出它在本课程中的使用场景。",
                List.of("学习间隔缩短", "本课程新增学习时长", "能够用自己的话解释一个概念"),
                "FALLBACK", "course-data-fallback"
        );
    }

    private EvidenceDraft fallbackEvidence(String answer) {
        if (answer.length() < 24) {
            return new EvidenceDraft("RETRY", "回答信息不足，建议回到对应章节补充一个概念、它的作用和一个课程内例子后再提交。", 20,
                    "FALLBACK", "course-data-fallback");
        }
        return new EvidenceDraft("TEACHER_REVIEW", "已收到学习说明。当前未连接模型服务，系统不会假装自动判定掌握程度，请教师结合回答进行复评。", 45,
                "FALLBACK", "course-data-fallback");
    }

    private void applyTeacherEdits(LearningPlanPO plan, LearningPlanDecisionRequest request) {
        if (StringUtils.hasText(request.getTitle())) {
            plan.setTitle(cleanText(request.getTitle(), 200));
        }
        if (StringUtils.hasText(request.getLearningGoal())) {
            plan.setLearningGoal(cleanText(request.getLearningGoal(), 1000));
        }
        if (request.getTaskSteps() != null && !request.getTaskSteps().isEmpty()) {
            List<String> steps = trimList(request.getTaskSteps(), 4);
            if (steps.size() < 2) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "学习计划至少需要两个可执行步骤");
            }
            plan.setTaskSteps(serializeList(steps));
        }
        if (request.getDurationMinutes() != null) {
            if (request.getDurationMinutes() < 10 || request.getDurationMinutes() > 60) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "微计划时长应在10到60分钟之间");
            }
            plan.setDurationMinutes(request.getDurationMinutes());
        }
        if (StringUtils.hasText(request.getAcceptanceCriteria())) {
            plan.setAcceptanceCriteria(cleanText(request.getAcceptanceCriteria(), 1000));
        }
    }

    private void validatePlan(PlanDraft plan) {
        if (!StringUtils.hasText(plan.diagnosis()) || !StringUtils.hasText(plan.goal()) || plan.steps().size() < 2
                || plan.durationMinutes() < 10 || plan.durationMinutes() > 60 || !StringUtils.hasText(plan.acceptanceCriteria())
                || !StringUtils.hasText(plan.checkQuestion()) || plan.expectedSignals().isEmpty()) {
            throw new BaseException(HttpStatus.BAD_GATEWAY, "AI生成计划不完整，已拒绝保存");
        }
    }

    private void validateEvidenceAssessment(EvidenceDraft assessment) {
        if (!Set.of("MASTERED", "RETRY", "TEACHER_REVIEW").contains(assessment.result())
                || !StringUtils.hasText(assessment.assessment()) || assessment.confidence() < 0 || assessment.confidence() > 100) {
            throw new BaseException(HttpStatus.BAD_GATEWAY, "AI理解检查结果不完整，已拒绝保存");
        }
    }

    private LearningCasePO requireCase(Long caseId) {
        LearningCasePO learningCase = caseMapper.selectById(caseId);
        if (learningCase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "学习诊断案例不存在");
        }
        return learningCase;
    }

    private LearningPlanPO requirePlan(Long planId) {
        LearningPlanPO plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "学习计划不存在");
        }
        return plan;
    }

    private LearningPlanPO requireLatestPlan(Long caseId) {
        LearningPlanPO plan = planMapper.selectOne(new LambdaQueryWrapper<LearningPlanPO>()
                .eq(LearningPlanPO::getCaseId, caseId).orderByDesc(LearningPlanPO::getUpdatedAt).orderByDesc(LearningPlanPO::getId).last("LIMIT 1"));
        if (plan == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "学习案例未找到对应计划");
        }
        return plan;
    }

    private LearningEvidencePO latestEvidence(Long planId) {
        return evidenceMapper.selectOne(new LambdaQueryWrapper<LearningEvidencePO>()
                .eq(LearningEvidencePO::getPlanId, planId).orderByDesc(LearningEvidencePO::getSubmittedAt).orderByDesc(LearningEvidencePO::getId).last("LIMIT 1"));
    }

    private EduClassPO requireTeacherClass(Long classId) {
        UserInfoDTO teacher = requireTeacher();
        EduClassPO clazz = classRepository.selectClassById(classId);
        if (clazz == null || !Objects.equals(clazz.getTeacherId(), teacher.getUserId())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权查看或处理该班级的学情");
        }
        return clazz;
    }

    private UserInfoDTO requireTeacher() {
        UserInfoDTO user = SecurityUtil.getLoginUser(UserInfoDTO.class);
        if (user == null || !ROLE_TEACHER.equals(user.getRoleCode())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "仅教师可使用AI学情干预中心");
        }
        return user;
    }

    private UserInfoDTO requireStudent() {
        UserInfoDTO user = SecurityUtil.getLoginUser(UserInfoDTO.class);
        if (user == null || !ROLE_STUDENT.equals(user.getRoleCode())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "仅学生可查看学习成长计划");
        }
        return user;
    }

    private List<EduChapterPO> activeChapters(Long courseId) {
        return chapterRepository.selectChaptersByCourseId(courseId).stream()
                .filter(item -> !Integer.valueOf(1).equals(item.getDeleted()))
                .sorted(Comparator.comparing(EduChapterPO::getSort, Comparator.nullsLast(Integer::compareTo)).thenComparing(EduChapterPO::getId))
                .toList();
    }

    private boolean isActiveCourse(EduCoursePO course) {
        return course != null && !Integer.valueOf(1).equals(course.getDeleted()) && !Integer.valueOf(0).equals(course.getStatus());
    }

    private int averageProgress(Collection<CourseSnapshot> snapshots) {
        return snapshots.isEmpty() ? 0 : (int) Math.round(snapshots.stream().mapToInt(CourseSnapshot::progress).average().orElse(0));
    }

    private EduStudyRecordPO newerRecord(EduStudyRecordPO first, EduStudyRecordPO second) {
        if (first.getLastStudyTime() == null) {
            return second;
        }
        if (second.getLastStudyTime() == null) {
            return first;
        }
        return second.getLastStudyTime().isAfter(first.getLastStudyTime()) ? second : first;
    }

    private List<LearningTeacherOverviewVO.RiskFactor> toRiskFactors(List<LearningRiskModel.LearningRiskFactor> factors) {
        return factors.stream().map(item -> LearningTeacherOverviewVO.RiskFactor.builder()
                .code(item.code()).label(item.label()).weight(item.weight()).evidence(item.evidence()).build()).toList();
    }

    private String buildCourseContext(CourseSnapshot snapshot) {
        String chapters = snapshot.chapters().stream().map(item -> item.getSort() + "." + item.getChapterName()).collect(Collectors.joining("；"));
        return "课程：" + snapshot.course().getCourseName()
                + "\n课程简介：" + defaultString(snapshot.course().getIntro(), "未提供课程简介")
                + "\n课程章节：" + abbreviate(chapters, 900)
                + "\n当前学生下一章节：" + snapshot.nextChapter()
                + "\n真实学习记录：完成度" + snapshot.progress() + "%；已完成" + snapshot.finishedChapters() + "/" + snapshot.chapters().size()
                + "章节；累计学习" + snapshot.studyMinutes() + "分钟；距上次学习" + (snapshot.idleDays() == null ? "暂无记录" : snapshot.idleDays() + "天");
    }

    private String buildRiskSummary(CourseSnapshot snapshot, int average, LearningRiskModel.LearningRiskResult risk) {
        String factors = risk.factors().stream().limit(3).map(item -> item.label() + "：" + item.evidence()).collect(Collectors.joining("；"));
        return "行为风险分：" + risk.score() + "（" + risk.level() + "）\n班级同课程平均进度：" + average + "%\n关键证据：" + factors;
    }

    private String serializeSnapshot(CourseSnapshot snapshot, int average) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("progress", snapshot.progress());
        values.put("studyMinutes", snapshot.studyMinutes());
        values.put("finishedChapters", snapshot.finishedChapters());
        values.put("courseAverage", average);
        values.put("idleDays", snapshot.idleDays());
        values.put("lastStudyTime", formatDateTime(snapshot.lastStudyTime()));
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception exception) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "无法保存学习行为快照");
        }
    }

    private int snapshotNumber(String snapshot, String field) {
        try {
            return objectMapper.readTree(snapshot).path(field).asInt(0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String serializeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception exception) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "无法保存计划步骤");
        }
    }

    private List<String> parseList(String value) {
        try {
            JsonNode node = objectMapper.readTree(value);
            List<String> values = new ArrayList<>();
            if (node.isArray()) {
                node.forEach(item -> {
                    if (StringUtils.hasText(item.asText())) {
                        values.add(item.asText());
                    }
                });
            }
            return values;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> trimList(List<String> values, int max) {
        return values == null ? List.of() : values.stream().filter(StringUtils::hasText)
                .map(value -> cleanText(value, 300)).limit(max).toList();
    }

    private void saveTrace(Long caseId, Long planId, Long studentId, String operation, String modelName, String source, String context, long elapsed) {
        traceMapper.insert(LearningAiTracePO.builder().caseId(caseId).planId(planId).studentId(studentId).operation(operation)
                .modelName(modelName).source(source).contextSummary(abbreviate(context, 1000)).elapsedMillis(elapsed)
                .createdAt(LocalDateTime.now()).build());
    }

    private String buildPlanTitle(CourseSnapshot snapshot, String goal) {
        return abbreviate("完成“" + snapshot.nextChapter() + "”微计划：" + goal, 200);
    }

    private String summarizeContext(CourseSnapshot snapshot) {
        return snapshot.course().getCourseName() + " / " + snapshot.nextChapter() + " / 进度" + snapshot.progress() + "%";
    }

    private String buildImprovementSummary(int progressChange, int minutesChange, LearningEvidencePO evidence) {
        String movement = "相较计划生成时，课程进度" + (progressChange >= 0 ? "+" : "") + progressChange
                + "%、学习时长" + (minutesChange >= 0 ? "+" : "") + minutesChange + "分钟。";
        if (evidence == null) {
            return movement + "等待学生提交学习证据。";
        }
        return movement + "理解检查结果：" + evidence.getResult() + "。";
    }

    private String cleanDecision(String value) {
        String decision = defaultString(value).trim().toUpperCase();
        if (!Set.of("ADOPT", "EDIT", "REJECT").contains(decision)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "计划处理方式无效");
        }
        return decision;
    }

    private String cleanOutcome(String value) {
        String outcome = defaultString(value).trim().toUpperCase();
        if (!Set.of(CASE_EFFECTIVE, CASE_CONTINUE, CASE_ESCALATE).contains(outcome)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "干预结论无效");
        }
        return outcome;
    }

    private String defaultConclusion(String outcome) {
        return switch (outcome) {
            case CASE_EFFECTIVE -> "学生已完成本轮目标，学习证据和行为变化符合预期。";
            case CASE_CONTINUE -> "学生已有学习证据，但需要继续练习后再次复评。";
            default -> "当前微计划未能解决学习困难，建议转入人工跟进。";
        };
    }

    private int normalizeProgress(Integer value) {
        return Math.max(0, Math.min(100, defaultNumber(value)));
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer daysSince(LocalDateTime value) {
        return value == null ? null : (int) Math.max(0, ChronoUnit.DAYS.between(value.toLocalDate(), LocalDateTime.now().toLocalDate()));
    }

    private Integer daysUntil(LocalDateTime value) {
        return value == null ? null : (int) ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), value.toLocalDate());
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String cleanText(String value, int maxLength) {
        String result = defaultString(value).trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(result)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "内容不能为空");
        }
        return abbreviate(result, maxLength);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private record ClassAnalysis(List<EduClassStudentPO> students, int courseCount, int averageProgress, int activeStudents,
                                 List<LearningTeacherOverviewVO.StudentRisk> risks, LearningCourseProfileVO classProfile,
                                 List<LearningStudentTypeProfileVO> studentProfiles,
                                 Map<Long, List<CourseSnapshot>> snapshotsByStudent, Map<Long, SysUserPO> users) {
    }

    private record StudentLearningState(String code, String label, String reason, String action) {
    }

    private record CourseContext(EduClassPO clazz, EduCoursePO course, EduCourseClassPO assignment, List<EduChapterPO> chapters) {
    }

    private record CourseSnapshot(
            Long studentId, SysUserPO student, EduClassPO clazz, EduCoursePO course, EduCourseClassPO assignment,
            List<EduChapterPO> chapters, int progress, int finishedChapters, int studyMinutes, LocalDateTime lastStudyTime,
            Integer idleDays, Long nextChapterId, String nextChapter
    ) {
    }

    private record PlanDraft(
            String diagnosis, String goal, List<String> steps, int durationMinutes, String acceptanceCriteria,
            String checkQuestion, List<String> expectedSignals, String source, String modelName
    ) {
    }

    private record EvidenceDraft(String result, String assessment, int confidence, String source, String modelName) {
    }

    private record RecommendationCandidate(EduCoursePO course) {
    }

    private record RecommendationDraft(EduCoursePO course, int score, String reason, String source, String modelName) {
    }
}
