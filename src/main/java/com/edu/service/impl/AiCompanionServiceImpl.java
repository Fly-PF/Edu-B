package com.edu.service.impl;

import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.ai.AiCompanionExchangeRequest;
import com.edu.pojo.dto.ai.AiCompanionSessionCreateRequest;
import com.edu.pojo.po.AiCompanionMessagePO;
import com.edu.pojo.po.AiCompanionSessionPO;
import com.edu.pojo.vo.ai.AiCompanionContextVO;
import com.edu.pojo.vo.ai.AiCompanionMaterialExcerpt;
import com.edu.pojo.vo.ai.AiCompanionWebSource;
import com.edu.pojo.vo.ai.AiCompanionStudySuggestion;
import com.edu.pojo.vo.ai.AiCompanionMessageVO;
import com.edu.pojo.vo.ai.AiCompanionModelResult;
import com.edu.pojo.vo.ai.AiCompanionSessionVO;
import com.edu.pojo.vo.course.ChapterVO;
import com.edu.pojo.vo.course.CourseStudyRecordVO;
import com.edu.pojo.vo.course.CourseVO;
import com.edu.pojo.vo.course.ResourceVO;
import com.edu.repository.AiCompanionRepository;
import com.edu.service.AiCompanionModelService;
import com.edu.service.AiCompanionSafetyPolicy;
import com.edu.service.AiCompanionService;
import com.edu.service.CourseService;
import com.edu.service.CourseMaterialRetrievalService;
import com.edu.service.AiCompanionWebSearchService;
import com.edu.service.RagService;
import com.edu.service.safety.SafetyGatewaySupport;
import com.edu.util.SecurityUtil;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCompanionServiceImpl implements AiCompanionService {
    private static final String ROLE_STUDENT = "STUDENT";
    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final int MAX_ANSWER_LENGTH = 12000;

    private final AiCompanionRepository companionRepository;
    private final CourseService courseService;
    private final AiCompanionModelService modelService;
    private final CourseMaterialRetrievalService materialRetrievalService;
    private final AiCompanionWebSearchService webSearchService;
    private final RagService ragService;
    private final SafetyGatewaySupport safetyGatewaySupport;

    @Override
    public AiCompanionContextVO getContext(Long courseId, Long chapterId, Long resourceId) {
        requireStudent();
        if (courseId == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程ID不能为空");
        }

        CourseVO course = courseService.getCourse(courseId);
        List<ChapterVO> chapters = courseService.listCourseChapters(courseId);
        ChapterVO chapter = selectChapter(chapters, chapterId);
        List<ResourceVO> resources = chapter == null
                ? List.of()
                : courseService.listChapterResources(chapter.getId());
        ResourceVO resource = selectResource(resources, resourceId);
        List<CourseStudyRecordVO> records = courseService.listStudyRecords(courseId);
        Map<Long, CourseStudyRecordVO> recordByChapter = records.stream()
                .collect(Collectors.toMap(CourseStudyRecordVO::getChapterId, Function.identity(), (left, right) -> right));
        CourseStudyRecordVO currentRecord = chapter == null ? null : recordByChapter.get(chapter.getId());
        ChapterVO nextChapter = chapters.stream()
                .filter(item -> {
                    CourseStudyRecordVO record = recordByChapter.get(item.getId());
                    return record == null || !Objects.equals(record.getFinishStatus(), 1);
                })
                .findFirst()
                .orElse(null);

        return AiCompanionContextVO.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .courseIntro(course.getIntro())
                .chapterId(chapter == null ? null : chapter.getId())
                .chapterTitle(chapter == null ? null : chapter.getTitle())
                .resourceId(resource == null ? null : resource.getId())
                .resourceName(resource == null ? null : resource.getName())
                .resourceType(resource == null ? null : resource.getType())
                .progress(currentRecord == null ? 0 : defaultNumber(currentRecord.getProgress()))
                .finishStatus(currentRecord == null ? 0 : defaultNumber(currentRecord.getFinishStatus()))
                .completedChapterCount((int) chapters.stream()
                        .filter(item -> {
                            CourseStudyRecordVO record = recordByChapter.get(item.getId());
                            return record != null && Objects.equals(record.getFinishStatus(), 1);
                        })
                        .count())
                .totalChapterCount(chapters.size())
                .chapterTitles(chapters.stream().map(ChapterVO::getTitle).toList())
                .resourceNames(resources.stream().map(ResourceVO::getName).toList())
                .nextChapterId(nextChapter == null ? null : nextChapter.getId())
                .nextChapterTitle(nextChapter == null ? null : nextChapter.getTitle())
                .personalizedSuggestions(buildPersonalizedSuggestions(
                        chapter, resources, currentRecord, nextChapter, records,
                        (int) chapters.stream().filter(item -> {
                            CourseStudyRecordVO record = recordByChapter.get(item.getId());
                            return record != null && Objects.equals(record.getFinishStatus(), 1);
                        }).count()
                ))
                .build();
    }

    private List<AiCompanionStudySuggestion> buildPersonalizedSuggestions(
            ChapterVO currentChapter,
            List<ResourceVO> currentResources,
            CourseStudyRecordVO currentRecord,
            ChapterVO nextChapter,
            List<CourseStudyRecordVO> records,
            int completedChapterCount
    ) {
        List<AiCompanionStudySuggestion> suggestions = new ArrayList<>();
        int progress = currentRecord == null ? 0 : defaultNumber(currentRecord.getProgress());
        boolean currentCompleted = currentRecord != null && Objects.equals(currentRecord.getFinishStatus(), 1);

        if (currentChapter != null && !currentCompleted) {
            suggestions.add(new AiCompanionStudySuggestion(
                    "CONTINUE",
                    "继续当前章节",
                    "“" + currentChapter.getTitle() + "”当前进度为 " + progress + "%",
                    "请结合当前章节，告诉我下一步应该学习什么，并给出 3 个具体步骤。"
            ));
        }
        if (currentCompleted && nextChapter != null) {
            suggestions.add(new AiCompanionStudySuggestion(
                    "NEXT",
                    "开始下一章节",
                    "你已完成当前章节，下一步学习“" + nextChapter.getTitle() + "”",
                    "请介绍下一章节“" + nextChapter.getTitle() + "”的学习目标和学习顺序。"
            ));
        }
        if (currentResources != null && !currentResources.isEmpty()) {
            ResourceVO resource = currentResources.getFirst();
            suggestions.add(new AiCompanionStudySuggestion(
                    "RESOURCE",
                    "学习本章资源",
                    "建议结合“" + resource.getName() + "”巩固当前章节",
                    "请说明当前资源“" + resource.getName() + "”的学习重点，以及我学习时应注意什么。"
            ));
        }

        LocalDateTime latestStudyTime = records == null ? null : records.stream()
                .map(CourseStudyRecordVO::getLastStudyTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (latestStudyTime != null && Duration.between(latestStudyTime, LocalDateTime.now()).toDays() >= 3) {
            long days = Duration.between(latestStudyTime, LocalDateTime.now()).toDays();
            suggestions.add(new AiCompanionStudySuggestion(
                    "REMINDER",
                    "继续学习提醒",
                    "你已 " + days + " 天没有学习这门课程了，建议用 10 分钟回顾后继续。",
                    "我有一段时间没有学习了，请为我安排一个 10 分钟的课程复习和继续学习计划。"
            ));
        } else if (completedChapterCount > 0) {
            suggestions.add(new AiCompanionStudySuggestion(
                    "REVIEW",
                    "复习已学内容",
                    "你已完成 " + completedChapterCount + " 个章节，可以先做一次简短复习。",
                    "请根据我已完成的章节，为我生成一份简短复习清单。"
            ));
        }
        return suggestions.stream().limit(3).toList();
    }

    @Override
    @Transactional
    public AiCompanionSessionVO createSession(AiCompanionSessionCreateRequest request) {
        UserInfoDTO student = requireStudent();
        if (request == null || request.getCourseId() == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程ID不能为空");
        }

        CourseVO course = courseService.getCourse(request.getCourseId());
        validateChapter(request.getCourseId(), request.getChapterId());
        LocalDateTime now = LocalDateTime.now();
        String title = normalizeTitle(request.getTitle(), course.getTitle());
        AiCompanionSessionPO session = AiCompanionSessionPO.builder()
                .studentId(student.getUserId())
                .courseId(request.getCourseId())
                .chapterId(request.getChapterId())
                .title(title)
                .lastMessageTime(now)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        companionRepository.insertSession(session);
        return toSessionVO(session);
    }

    @Override
    public List<AiCompanionSessionVO> listSessions(Long courseId) {
        UserInfoDTO student = requireStudent();
        if (courseId != null) {
            courseService.getCourse(courseId);
        }
        return companionRepository.selectSessionsByStudentId(student.getUserId(), courseId).stream()
                .map(this::toSessionVO)
                .toList();
    }

    @Override
    @Transactional
    public List<AiCompanionMessageVO> appendExchange(Long sessionId, AiCompanionExchangeRequest request) {
        UserInfoDTO student = requireStudent();
        AiCompanionSessionPO session = requireOwnedSession(sessionId, student.getUserId());
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "问题内容不能为空");
        }

        String question = request.getQuestion().trim();
        validateLength(question, MAX_QUESTION_LENGTH, "问题内容过长");

        String safeQuestion = safetyGatewaySupport.enforceInputText(
                SafetySourceModule.AI_COMPANION,
                SafetyScene.STUDENT_AI,
                null,
                question,
                Map.of(
                        "entryPoint", "ai-companion-service",
                        "sessionId", String.valueOf(sessionId),
                        "courseId", String.valueOf(session.getCourseId())
                )
        );
        Long chapterId = request.getChapterId() == null ? session.getChapterId() : request.getChapterId();
        validateChapter(session.getCourseId(), chapterId);
        validateResource(chapterId, request.getResourceId());

        AiCompanionContextVO context = getContext(session.getCourseId(), chapterId, request.getResourceId());
        List<AiCompanionMessageVO> history = companionRepository.selectMessagesBySessionId(sessionId).stream()
                .map(this::toMessageVO)
                .toList();
        long generationStartedAt = System.nanoTime();
        AiCompanionSafetyPolicy.SafetyDecision safetyDecision = AiCompanionSafetyPolicy.check(safeQuestion);
        if (!safetyDecision.blocked()) {
            List<AiCompanionMaterialExcerpt> materials = ragService.retrieveCourseMaterials(session.getCourseId(), safeQuestion);
            if (materials.isEmpty()) {
                // A student may ask about a concept taught in another chapter, so search the
                // whole enrolled course rather than only the resource currently on screen.
                List<ResourceVO> courseResources = courseService.listCourseChapters(session.getCourseId()).stream()
                        .flatMap(chapter -> courseService.listChapterResources(chapter.getId()).stream())
                        .toList();
                materials = materialRetrievalService.retrieve(courseResources, safeQuestion);
            }
            context.setMatchedMaterials(materials);
            if (materials.isEmpty()) {
                List<AiCompanionWebSource> webSources = webSearchService.search(safeQuestion);
                context.setWebSources(webSources);
            }
        }
        AiCompanionModelResult modelResult = safetyDecision.blocked()
                ? blockedResult(context, safetyDecision)
                : modelService.generateAnswer(context, history, safeQuestion);
        long responseTimeMs = Math.max(0, (System.nanoTime() - generationStartedAt) / 1_000_000);
        String answer = modelResult.content();
        validateLength(answer, MAX_ANSWER_LENGTH, "回答内容过长");

        LocalDateTime now = LocalDateTime.now();
        List<AiCompanionMessageVO> saved = new ArrayList<>();
        saved.add(saveMessage(session, student.getUserId(), "USER", safeQuestion, chapterId, request.getResourceId(), null, null, null, safetyDecision.status(), null, now));
        saved.add(saveMessage(session, student.getUserId(), "ASSISTANT", answer, chapterId, request.getResourceId(), modelResult.mode(), modelResult.modelName(), modelResult.sourceSummary(), modelResult.safetyStatus(), responseTimeMs, now.plusNanos(1)));
        companionRepository.updateSessionActivity(sessionId, now);
        return saved;
    }

    @Override
    public List<AiCompanionMessageVO> listMessages(Long sessionId) {
        UserInfoDTO student = requireStudent();
        requireOwnedSession(sessionId, student.getUserId());
        return companionRepository.selectMessagesBySessionId(sessionId).stream()
                .map(this::toMessageVO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteConversation(Long sessionId) {
        UserInfoDTO student = requireStudent();
        requireOwnedSession(sessionId, student.getUserId());
        companionRepository.deleteMessagesBySessionId(sessionId);
        companionRepository.deleteSessionByIdAndStudentId(sessionId, student.getUserId());
    }

    @Override
    @Transactional
    public void clearConversations() {
        UserInfoDTO student = requireStudent();
        companionRepository.deleteMessagesByStudentId(student.getUserId());
        companionRepository.deleteSessionsByStudentId(student.getUserId());
    }

    private AiCompanionMessageVO saveMessage(
            AiCompanionSessionPO session,
            Long studentId,
            String role,
            String content,
            Long chapterId,
            Long resourceId,
            String generationMode,
            String modelName,
            String sourceSummary,
            String safetyStatus,
            Long responseTimeMs,
            LocalDateTime createTime
    ) {
        AiCompanionMessagePO message = AiCompanionMessagePO.builder()
                .sessionId(session.getId())
                .studentId(studentId)
                .role(role)
                .content(content)
                .chapterId(chapterId)
                .resourceId(resourceId)
                .generationMode(generationMode)
                .modelName(modelName)
                .sourceSummary(sourceSummary)
                .safetyStatus(safetyStatus)
                .responseTimeMs(responseTimeMs)
                .createTime(createTime)
                .deleted(0)
                .build();
        companionRepository.insertMessage(message);
        return toMessageVO(message);
    }

    private AiCompanionModelResult blockedResult(
            AiCompanionContextVO context,
            AiCompanionSafetyPolicy.SafetyDecision decision
    ) {
        return new AiCompanionModelResult(
                decision.response(),
                "SAFETY_BLOCKED",
                null,
                buildSourceSummary(context),
                decision.status()
        );
    }

    private String buildSourceSummary(AiCompanionContextVO context) {
        if (context.getMatchedMaterials() != null && !context.getMatchedMaterials().isEmpty()) {
            AiCompanionMaterialExcerpt material = context.getMatchedMaterials().getFirst();
            return "课程：" + safe(context.getCourseTitle())
                    + " / 章节：" + safe(context.getChapterTitle())
                    + " / 资料：" + safe(material.resourceName())
                    + " 第" + material.pageNumber() + "页";
        }
        if (context.getWebSources() != null && !context.getWebSources().isEmpty()) {
            return "本课程教案未命中 / 联网资料：" + safe(context.getWebSources().getFirst().title());
        }
        return "课程资料未覆盖 / 通用学习参考";
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "未提供";
    }

    private void validateChapter(Long courseId, Long chapterId) {
        if (chapterId == null) return;
        boolean exists = courseService.listCourseChapters(courseId).stream()
                .map(ChapterVO::getId)
                .anyMatch(id -> Objects.equals(id, chapterId));
        if (!exists) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "章节不属于当前课程");
        }
    }

    private ChapterVO selectChapter(List<ChapterVO> chapters, Long chapterId) {
        if (chapterId == null) {
            return chapters.isEmpty() ? null : chapters.getFirst();
        }
        return chapters.stream()
                .filter(item -> Objects.equals(item.getId(), chapterId))
                .findFirst()
                .orElseThrow(() -> new BaseException(HttpStatus.BAD_REQUEST, "章节不属于当前课程"));
    }

    private ResourceVO selectResource(List<ResourceVO> resources, Long resourceId) {
        if (resourceId == null) {
            return resources.isEmpty() ? null : resources.getFirst();
        }
        return resources.stream()
                .filter(item -> Objects.equals(item.getId(), resourceId))
                .findFirst()
                .orElseThrow(() -> new BaseException(HttpStatus.BAD_REQUEST, "资源不属于当前章节"));
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private void validateResource(Long chapterId, Long resourceId) {
        if (resourceId == null) return;
        if (chapterId == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "资源必须关联章节");
        }
        boolean exists = courseService.listChapterResources(chapterId).stream()
                .anyMatch(resource -> Objects.equals(resource.getId(), resourceId));
        if (!exists) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "资源不属于当前章节");
        }
    }

    private AiCompanionSessionPO requireOwnedSession(Long sessionId, Long studentId) {
        if (sessionId == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "会话ID不能为空");
        }
        AiCompanionSessionPO session = companionRepository.selectSessionById(sessionId);
        if (session == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "智能学伴会话不存在");
        }
        if (!Objects.equals(session.getStudentId(), studentId)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权访问该会话");
        }
        courseService.getCourse(session.getCourseId());
        return session;
    }

    private UserInfoDTO requireStudent() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (!ROLE_STUDENT.equals(user.getRoleCode())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "仅学生可以使用智能学伴");
        }
        return user;
    }

    private String normalizeTitle(String requestedTitle, String courseTitle) {
        String title = StringUtils.hasText(requestedTitle) ? requestedTitle.trim() : courseTitle + "学习对话";
        return title.length() <= 100 ? title : title.substring(0, 100);
    }

    private void validateLength(String content, int maxLength, String message) {
        if (content.length() > maxLength) {
            throw new BaseException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private AiCompanionSessionVO toSessionVO(AiCompanionSessionPO session) {
        return AiCompanionSessionVO.builder()
                .id(session.getId())
                .courseId(session.getCourseId())
                .chapterId(session.getChapterId())
                .title(session.getTitle())
                .lastMessageTime(session.getLastMessageTime())
                .createTime(session.getCreateTime())
                .build();
    }

    private AiCompanionMessageVO toMessageVO(AiCompanionMessagePO message) {
        return AiCompanionMessageVO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .chapterId(message.getChapterId())
                .resourceId(message.getResourceId())
                .generationMode(message.getGenerationMode())
                .modelName(message.getModelName())
                .sourceSummary(message.getSourceSummary())
                .safetyStatus(message.getSafetyStatus())
                .responseTimeMs(message.getResponseTimeMs())
                .createTime(message.getCreateTime())
                .build();
    }
}
