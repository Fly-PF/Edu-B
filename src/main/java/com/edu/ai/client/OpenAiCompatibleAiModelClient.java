package com.edu.ai.client;

import com.edu.exception.UserErrorException;
import com.edu.common.properties.AiProviderProperties;
import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingDimensionScore;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanExercise;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanTeachingStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@ConditionalOnProperty(name = "edu.ai.provider", havingValue = "openai-compatible")
@Slf4j
public class OpenAiCompatibleAiModelClient implements AiModelClient {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final int DEBUG_CONTENT_LIMIT = 1500;
    private static final int GRADING_MAX_TOKENS = 420;
    private static final int LESSON_PLAN_MAX_TOKENS = 900;
    private static final BigDecimal GRADING_TEMPERATURE = BigDecimal.ZERO;
    private static final BigDecimal LESSON_PLAN_TEMPERATURE = BigDecimal.ZERO;
    private static final int MAX_GRADING_FEEDBACK_ITEMS = 2;
    private static final int GRADING_CACHE_MAX_ENTRIES = 100;
    private static final int LESSON_PLAN_CACHE_MAX_ENTRIES = 100;
    private static final Duration GRADING_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration LESSON_PLAN_CACHE_TTL = Duration.ofMinutes(30);
    private static final int DEFAULT_GRADING_TIMEOUT_MS = 30000;
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
    private static final String GRADING_PROMPT_VERSION = "grading-v4-compact-cache";
    private static final String LESSON_PLAN_PROMPT_VERSION = "lesson-plan-v2-compact-cache";
    private static final String GRADING_OPERATION = "grading";
    private static final String LESSON_PLAN_OPERATION = "lesson-plan";

    private final RestClient.Builder restClientBuilder;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiProviderProperties properties;
    private final Map<String, CachedGradingResponse> gradingCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedGradingResponse> eldest) {
            return size() > GRADING_CACHE_MAX_ENTRIES;
        }
    };
    private final Map<String, CachedLessonPlanResponse> lessonPlanCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedLessonPlanResponse> eldest) {
            return size() > LESSON_PLAN_CACHE_MAX_ENTRIES;
        }
    };

    public OpenAiCompatibleAiModelClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AiProviderProperties properties
    ) {
        this.restClientBuilder = restClientBuilder;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public LessonPlanGenerateResponse generateLessonPlan(LessonPlanGenerateRequest request) {
        String model = resolveLessonModel();
        String cacheKey = buildLessonPlanCacheKey(request, model);
        CachedLessonPlanResponse cached = lookupCachedLessonPlanResponse(cacheKey);
        if (cached != null) {
            log.info("lesson-plan ai cache hit, provider={}, model={}, maxTokens={}", resolveProvider(), model, LESSON_PLAN_MAX_TOKENS);
            return copyLessonPlanResponse(cached.response());
        }
        log.info("lesson-plan ai cache miss, provider={}, model={}, maxTokens={}", resolveProvider(), model, LESSON_PLAN_MAX_TOKENS);
        CompletionResult result = requestCompletion(
                LESSON_PLAN_OPERATION,
                model,
                lessonPlanSystemPrompt(),
                buildLessonPlanPromptV2(request),
                LESSON_PLAN_MAX_TOKENS,
                LESSON_PLAN_TEMPERATURE,
                null
        );
        try {
            LessonPlanGenerateResponse response = objectMapper.treeToValue(result.content(), LessonPlanGenerateResponse.class);
            normalizeLessonPlanResponse(response);
            if (isLessonPlanCacheable(request, response)) {
                storeCachedLessonPlanResponse(cacheKey, response);
            }
            return response;
        } catch (JsonProcessingException exception) {
            throw invalidModelResponse("教案 JSON 结构无法解析", exception);
        }
    }

    @Override
    public GradingGenerateResponse generateGrading(GradingGenerateRequest request) {
        return generateGradingWithRetryV2(request);
    }

    String buildGradingPrompt(GradingGenerateRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("question", request.getQuestion());
        input.put("questionType", request.getQuestionType());
        input.put("referenceAnswer", request.getReferenceAnswer());
        input.put("rubric", request.getRubric());
        input.put("studentAnswer", request.getStudentAnswer());
        input.put("maxScore", request.getMaxScore());
        if (request != null) {
            return buildCompactGradingPrompt(request);
        }
        String gradingFullMarkGuard = "Before giving full marks for any dimension, run a completeness check: every key requirement in rubric.description must be supported by explicit or semantic evidence from the student answer. If any required part is missing, that dimension cannot receive full marks. Partial coverage, a more generic statement, or giving only one side of a comparison is not enough for full marks. Saying \"both can store many items\" is not enough to prove \"both are ordered sequences\". Giving only one usage scenario is not enough to prove that both data structures have been matched to appropriate scenarios. Do not use keyword matching; use semantic evidence, but require complete coverage for full marks.";
        return "请只依据以下 JSON 输入完成批改：\n"
                + writeJson(input)
                + "\n"
                + gradingFullMarkGuard
                + "\n只返回严格 JSON，不能输出 Markdown、代码围栏或解释文本。\n"
                + "评分规则：\n"
                + "1. 先识别学生明确写出的证据，再结合 referenceAnswer 与 rubric.description 判断覆盖程度。\n"
                + "2. 同义改写算覆盖，但重要知识点未表达就必须扣分；不能因为“核心方向正确”就自动满分。\n"
                + "3. 先定档位再给分：100%=主要要求全部满足且无实质遗漏；85%-95%=核心正确，仅轻微遗漏；65%-85%=核心正确但有一个或多个重要要点缺失；40%-65%=部分正确；0%-40%=明显错误或大面积缺失。\n"
                + "4. 满分检查：只有在重要知识点基本完整覆盖、deductions 为空、且每个维度都真的完全满足时，totalScore 才能等于 maxScore。\n"
                + "5. 监督学习要看标签数据、输入到目标输出的映射关系，以及常见用途；无监督学习要看无标签数据、发现结构/规律，以及常见用途。只写“有标签/没标签”或“自己找规律”不算自动满分。\n"
                + "6. 案例匹配性：垃圾邮件分类属于监督学习，用户分群属于无监督学习。\n"
                + "7. 每个维度 reason 仅需 1-2 句，strengths/deductions/suggestions 每项最多 3 条，revisedAnswer 保持简洁。\n"
                + "8. confidence 表示评分可靠程度，不是答案好坏；边界清晰时通常 0.85-0.95，只有几乎无歧义且证据极充分时才可 >0.95。\n"
                + "9. 请严格返回以下 JSON Schema，不能缺字段：\n"
                + gradingJsonSchema()
                + "\n要求：strengths 必须存在；deductions 和 suggestions 没有内容时可以返回 []；"
                + "dimensionScores 的数量、名称、顺序必须与 Rubric 完全一致；"
                + "totalScore 必须等于所有 dimensionScores.score 之和；"
                + "不要因为学生没有逐字复述参考答案而扣分，必须优先判断语义等价。";
    }

    private String buildGradingRepairPrompt(GradingGenerateRequest request) {
        return buildGradingPrompt(request)
                + "\n上一份评分结果格式不符合协议，请保持原评分结论不变，只修复为指定 JSON Schema。";
    }

    private String buildLessonPlanPrompt(LessonPlanGenerateRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("courseId", request.getCourseId());
        input.put("topic", request.getTopic());
        input.put("grade", request.getGrade());
        input.put("durationMinutes", request.getDurationMinutes());
        input.put("objectives", request.getObjectives());
        input.put("difficulty", request.getDifficulty());
        input.put("requirements", request.getRequirements());
        return "请只依据以下 JSON 输入生成教案：\n"
                + writeJson(input)
                + "\n只返回 JSON 对象。字段必须包含 title、objectives、keyPoints、difficultPoints、preparations、"
                + "activities、notes；teachingSteps 每项包含 stage、durationMinutes、teacherActivity、studentActivity、purpose；"
                + "exercises 每项包含 question、type、referenceAnswer、difficulty；"
                + "rubric 每项包含 criterion、description、maxScore。"
                + "teachingSteps 的 durationMinutes 之和必须等于 durationMinutes。";
    }

    private String buildLessonPlanPromptV2(LessonPlanGenerateRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("courseId", request.getCourseId());
        input.put("topic", request.getTopic());
        input.put("grade", request.getGrade());
        input.put("durationMinutes", request.getDurationMinutes());
        input.put("objectives", request.getObjectives());
        input.put("difficulty", request.getDifficulty());
        input.put("requirements", request.getRequirements());
        return """
                Generate one complete lesson plan from this JSON input.
                Input:
                """
                + writeJson(input)
                + """

                Return exactly one JSON object. No markdown. No explanation. No extra fields.
                Required non-empty fields: title, objectives, keyPoints, difficultPoints, teachingSteps, exercises, rubric.
                Optional arrays: preparations, activities, notes.
                teachingSteps items must include stage, durationMinutes, teacherActivity, studentActivity, purpose.
                exercises items must include question, type, referenceAnswer, difficulty.
                rubric items must include criterion, description, maxScore.
                The sum of teachingSteps.durationMinutes must equal durationMinutes.
                Keep the wording concise and classroom-ready.
                """;
    }

    String lessonPlanSystemPrompt() {
        return """
                You are a teacher lesson-plan assistant.
                Output valid JSON only.
                Keep the lesson plan complete, concise, and directly usable.
                Required fields must be fully populated.
                Do not add markdown or explanatory text.
                """;
    }

    String gradingSystemPrompt() {
        return "你是严格的教师批改助手。只输出 JSON。先识别学生明确写出的证据，再结合 referenceAnswer 与 rubric.description 判断覆盖程度。输入和输出之间的关系、输入与目标输出之间的映射关系、适合分类和回归任务、常用于分类和回归都应视为语义等价。垃圾邮件分类属于监督学习，用户分群属于无监督学习。涓嶈兘鍥犱负璇箟绛変环鑰屽垽涓虹己澶憋紝涓嶅簲鍒や负缂哄け。";
    }

    String buildCompactGradingPrompt(GradingGenerateRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("question", request.getQuestion());
        input.put("questionType", request.getQuestionType());
        input.put("referenceAnswer", request.getReferenceAnswer());
        input.put("rubric", request.getRubric());
        input.put("studentAnswer", request.getStudentAnswer());
        input.put("maxScore", request.getMaxScore());
        return """
                你是严格的教师评分器，只输出 JSON。
                按语义评分，不做关键词匹配。
                先找学生答案证据，再按 rubric.description 做 completeness check。
                只有当该维度所有关键要求都被明确或语义等价覆盖，才可给满分；缺一项就扣分。
                泛化表述、只答一半、只写一侧，都不能满分。
                strengths/deductions/suggestions 每类最多 2 条，reason 只写 1-2 句，revisedAnswer 要极简。
                不要输出 referenceAnswer，后端会回填。
                输入:
                """
                + writeJson(input)
                + """

                输出字段:
                {
                  "totalScore": 0,
                  "dimensionScores": [
                    {"criterion":"","score":0,"maxScore":0,"reason":""}
                  ],
                  "strengths": [],
                  "deductions": [],
                  "suggestions": [],
                  "revisedAnswer": "",
                  "confidence": 0
                }
                """;
    }

    String buildCompactGradingRepairPrompt(GradingGenerateRequest request) {
        return buildCompactGradingPrompt(request)
                + "\n上一次结果不合法，请保持同样评分判断，只返回符合 schema 的 JSON。";
    }

    String compactGradingSystemPrompt() {
        return """
                你是严格的教师批改助手，只输出 JSON。
                按语义评分，不是关键词匹配。
                先找学生答案证据，再按 rubric.description 做 completeness check。
                满分前必须确认该维度所有关键要求都被学生答案明确或语义等价覆盖。
                缺失关键要求时必须扣分；泛化表述或只答一半都不能满分。
                """;
    }

    private GradingGenerateResponse generateGradingWithRetryV2(GradingGenerateRequest request) {
        String model = resolveGradingModel();
        String cacheKey = buildGradingCacheKey(request, model);
        CachedGradingResponse cached = lookupCachedGradingResponse(cacheKey);
        if (cached != null) {
            log.info("grading ai cache hit, provider={}, model={}, maxTokens={}", resolveProvider(), model, GRADING_MAX_TOKENS);
            return copyGradingResponse(cached.response());
        }

        log.info("grading ai cache miss, provider={}, model={}, maxTokens={}", resolveProvider(), model, GRADING_MAX_TOKENS);
        long startedAt = System.nanoTime();
        try {
            CompletionResult first = requestCompletion(
                    GRADING_OPERATION,
                    model,
                    compactGradingSystemPrompt(),
                    buildCompactGradingPrompt(request),
                    GRADING_MAX_TOKENS,
                    GRADING_TEMPERATURE,
                    resolveGradingTimeoutMs()
            );
            GradingGenerateResponse response = parseGradingResponseV2(request, model, first);
            storeCachedGradingResponse(cacheKey, response);
            return response;
        } catch (IllegalStateException firstFailure) {
            if (!shouldRetryGradingFailureV2(firstFailure)) {
                throw firstFailure;
            }
            CompletionResult retryResult = requestCompletion(
                    GRADING_OPERATION,
                    model,
                    compactGradingSystemPrompt(),
                    buildCompactGradingRepairPrompt(request),
                    GRADING_MAX_TOKENS,
                    GRADING_TEMPERATURE,
                    resolveGradingTimeoutMs()
            );
            GradingGenerateResponse response = parseGradingResponseV2(request, model, retryResult);
            storeCachedGradingResponse(cacheKey, response);
            return response;
        } finally {
            log.info("grading ai total cost={} ms, provider={}, model={}",
                    elapsedMillis(startedAt), resolveProvider(), model);
        }
    }

    private boolean shouldRetryGradingFailureV2(IllegalStateException exception) {
        String message = exception.getMessage();
        return StringUtils.hasText(message) && message.startsWith("AI模型返回结果无效");
    }

    private GradingGenerateResponse parseGradingResponseV2(
            GradingGenerateRequest request,
            String model,
            CompletionResult result
    ) {
        try {
            GradingGenerateResponse response = objectMapper.treeToValue(result.content(), GradingGenerateResponse.class);
            response.setReferenceAnswer(request.getReferenceAnswer());
            response.setProvider(resolveProvider());
            response.setModel(model);
            normalizeFeedbackLists(response);
            validateGradingResponseV2(request, response);
            return response;
        } catch (JsonProcessingException exception) {
            log.warn("AI grading invalid, provider={}, model={}, httpStatus={}, contentLength={}, reason={}",
                    resolveProvider(), model, result.httpStatus(), result.rawContent().length(), exception.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("AI grading content preview: {}", truncate(result.rawContent(), DEBUG_CONTENT_LIMIT));
            }
            throw invalidModelResponse("invalid JSON", exception);
        } catch (IllegalStateException exception) {
            log.warn("AI grading invalid, provider={}, model={}, httpStatus={}, contentLength={}, reason={}",
                    resolveProvider(), model, result.httpStatus(), result.rawContent().length(), exception.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("AI grading content preview: {}", truncate(result.rawContent(), DEBUG_CONTENT_LIMIT));
            }
            throw exception;
        }
    }

    private void validateGradingResponseV2(GradingGenerateRequest request, GradingGenerateResponse response) {
        if (response == null || response.getTotalScore() == null) {
            throw invalidModelResponse("grading totalScore missing", null);
        }
        if (!StringUtils.hasText(response.getReferenceAnswer())
                || !StringUtils.hasText(response.getRevisedAnswer())) {
            throw invalidModelResponse("referenceAnswer or revisedAnswer missing", null);
        }
        requireNonEmptyListV2(response.getDimensionScores(), "dimensionScores");
        requireNonEmptyListV2(response.getStrengths(), "strengths");
        requirePresentListV2(response.getDeductions(), "deductions");
        requirePresentListV2(response.getSuggestions(), "suggestions");
        requireAtMostTwoItems(response.getStrengths(), "strengths");
        requireAtMostTwoItems(response.getDeductions(), "deductions");
        requireAtMostTwoItems(response.getSuggestions(), "suggestions");
        if (response.getDimensionScores().size() != request.getRubric().size()) {
            throw invalidModelResponse("dimensionScores size mismatch", null);
        }

        BigDecimal dimensionTotal = ZERO;
        for (int index = 0; index < request.getRubric().size(); index++) {
            AiRubricItem expected = request.getRubric().get(index);
            GradingDimensionScore actual = response.getDimensionScores().get(index);
            if (actual == null
                    || !StringUtils.hasText(actual.getCriterion())
                    || actual.getMaxScore() == null
                    || actual.getScore() == null
                    || !StringUtils.hasText(actual.getReason())) {
                throw invalidModelResponse("dimensionScores item invalid", null);
            }
            if (!expected.getCriterion().trim().equals(actual.getCriterion().trim())
                    || expected.getMaxScore().compareTo(actual.getMaxScore()) != 0) {
                throw invalidModelResponse("dimensionScores rubric mismatch", null);
            }
            if (actual.getScore().compareTo(ZERO) < 0
                    || actual.getScore().compareTo(actual.getMaxScore()) > 0
                    || !hasAtMostOneDecimal(actual.getScore())) {
                throw invalidModelResponse("dimension score out of range", null);
            }
            dimensionTotal = dimensionTotal.add(actual.getScore());
        }

        if (response.getTotalScore().compareTo(ZERO) < 0
                || response.getTotalScore().compareTo(request.getMaxScore()) > 0
                || !hasAtMostOneDecimal(response.getTotalScore())
                || dimensionTotal.compareTo(response.getTotalScore()) != 0) {
            throw invalidModelResponse("totalScore mismatch", null);
        }
        if (response.getConfidence() == null
                || response.getConfidence().compareTo(ZERO) < 0
                || response.getConfidence().compareTo(ONE) > 0) {
            throw invalidModelResponse("confidence out of range", null);
        }
    }

    private void normalizeFeedbackLists(GradingGenerateResponse response) {
        response.setStrengths(limitList(response.getStrengths(), MAX_GRADING_FEEDBACK_ITEMS));
        response.setDeductions(limitList(response.getDeductions(), MAX_GRADING_FEEDBACK_ITEMS));
        response.setSuggestions(limitList(response.getSuggestions(), MAX_GRADING_FEEDBACK_ITEMS));
    }

    private List<String> limitList(List<String> values, int limit) {
        if (values == null || values.size() <= limit) {
            return values;
        }
        return List.copyOf(values.subList(0, limit));
    }

    private String buildGradingCacheKey(GradingGenerateRequest request, String model) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("promptVersion", GRADING_PROMPT_VERSION);
        normalized.put("model", model);
        normalized.put("maxTokens", GRADING_MAX_TOKENS);
        normalized.put("question", normalizeText(request.getQuestion()));
        normalized.put("questionType", normalizeText(request.getQuestionType()));
        normalized.put("referenceAnswer", normalizeText(request.getReferenceAnswer()));
        normalized.put("studentAnswer", normalizeText(request.getStudentAnswer()));
        normalized.put("maxScore", request.getMaxScore() == null ? null : request.getMaxScore().stripTrailingZeros().toPlainString());
        List<Map<String, String>> rubric = new ArrayList<>();
        for (AiRubricItem item : request.getRubric()) {
            Map<String, String> rubricItem = new LinkedHashMap<>();
            rubricItem.put("criterion", normalizeText(item.getCriterion()));
            rubricItem.put("description", normalizeText(item.getDescription()));
            rubricItem.put("maxScore", item.getMaxScore() == null ? null : item.getMaxScore().stripTrailingZeros().toPlainString());
            rubric.add(rubricItem);
        }
        normalized.put("rubric", rubric);
        return sha256(writeJson(normalized));
    }

    private synchronized CachedGradingResponse lookupCachedGradingResponse(String key) {
        CachedGradingResponse cached = gradingCache.get(key);
        if (cached == null) {
            return null;
        }
        long ageMillis = Duration.between(cached.cachedAt(), Instant.now()).toMillis();
        if (ageMillis > GRADING_CACHE_TTL.toMillis()) {
            gradingCache.remove(key);
            return null;
        }
        return cached;
    }

    private synchronized void storeCachedGradingResponse(String key, GradingGenerateResponse response) {
        gradingCache.put(key, new CachedGradingResponse(Instant.now(), copyGradingResponse(response)));
    }

    private GradingGenerateResponse copyGradingResponse(GradingGenerateResponse response) {
        return objectMapper.convertValue(response, GradingGenerateResponse.class);
    }

    private String buildLessonPlanCacheKey(LessonPlanGenerateRequest request, String model) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("promptVersion", LESSON_PLAN_PROMPT_VERSION);
        normalized.put("model", model);
        normalized.put("maxTokens", LESSON_PLAN_MAX_TOKENS);
        normalized.put("courseId", request.getCourseId());
        normalized.put("topic", normalizeText(request.getTopic()));
        normalized.put("grade", normalizeText(request.getGrade()));
        normalized.put("durationMinutes", request.getDurationMinutes());
        normalized.put("objectives", normalizeText(request.getObjectives()));
        normalized.put("difficulty", normalizeText(request.getDifficulty()));
        normalized.put("requirements", normalizeText(request.getRequirements()));
        return sha256(writeJson(normalized));
    }

    private synchronized CachedLessonPlanResponse lookupCachedLessonPlanResponse(String key) {
        CachedLessonPlanResponse cached = lessonPlanCache.get(key);
        if (cached == null) {
            return null;
        }
        long ageMillis = Duration.between(cached.cachedAt(), Instant.now()).toMillis();
        if (ageMillis > LESSON_PLAN_CACHE_TTL.toMillis()) {
            lessonPlanCache.remove(key);
            return null;
        }
        return cached;
    }

    private synchronized void storeCachedLessonPlanResponse(String key, LessonPlanGenerateResponse response) {
        lessonPlanCache.put(key, new CachedLessonPlanResponse(Instant.now(), copyLessonPlanResponse(response)));
    }

    private LessonPlanGenerateResponse copyLessonPlanResponse(LessonPlanGenerateResponse response) {
        return objectMapper.convertValue(response, LessonPlanGenerateResponse.class);
    }

    private void normalizeLessonPlanResponse(LessonPlanGenerateResponse response) {
        if (response == null) {
            return;
        }
        response.setPreparations(defaultList(response.getPreparations()));
        response.setNotes(defaultList(response.getNotes()));
        if (response.getActivities() == null || response.getActivities().isEmpty()) {
            response.setActivities(buildActivitiesFromTeachingSteps(response.getTeachingSteps()));
        } else {
            response.setActivities(defaultList(response.getActivities()));
        }
    }

    private boolean isLessonPlanCacheable(LessonPlanGenerateRequest request, LessonPlanGenerateResponse response) {
        if (response == null || !StringUtils.hasText(response.getTitle())) {
            return false;
        }
        if (response.getTeachingSteps() == null || response.getTeachingSteps().isEmpty()) {
            return false;
        }
        if (response.getObjectives() == null || response.getObjectives().isEmpty()
                || response.getKeyPoints() == null || response.getKeyPoints().isEmpty()
                || response.getDifficultPoints() == null || response.getDifficultPoints().isEmpty()
                || response.getExercises() == null || response.getExercises().isEmpty()
                || response.getRubric() == null || response.getRubric().isEmpty()) {
            return false;
        }

        int durationTotal = 0;
        for (LessonPlanTeachingStep step : response.getTeachingSteps()) {
            if (step == null
                    || !StringUtils.hasText(step.getStage())
                    || step.getDurationMinutes() == null
                    || step.getDurationMinutes() <= 0
                    || !StringUtils.hasText(step.getTeacherActivity())
                    || !StringUtils.hasText(step.getStudentActivity())
                    || !StringUtils.hasText(step.getPurpose())) {
                return false;
            }
            durationTotal += step.getDurationMinutes();
        }
        if (!Objects.equals(durationTotal, request.getDurationMinutes())) {
            return false;
        }

        for (LessonPlanExercise exercise : response.getExercises()) {
            if (exercise == null
                    || !StringUtils.hasText(exercise.getQuestion())
                    || !StringUtils.hasText(exercise.getType())
                    || !StringUtils.hasText(exercise.getReferenceAnswer())
                    || !StringUtils.hasText(exercise.getDifficulty())) {
                return false;
            }
        }

        for (AiRubricItem rubricItem : response.getRubric()) {
            if (rubricItem == null
                    || !StringUtils.hasText(rubricItem.getCriterion())
                    || !StringUtils.hasText(rubricItem.getDescription())
                    || rubricItem.getMaxScore() == null) {
                return false;
            }
        }
        return true;
    }

    private List<String> buildActivitiesFromTeachingSteps(List<LessonPlanTeachingStep> teachingSteps) {
        if (teachingSteps == null || teachingSteps.isEmpty()) {
            return List.of();
        }
        List<String> activities = new ArrayList<>();
        for (LessonPlanTeachingStep step : teachingSteps) {
            if (step == null) {
                continue;
            }
            String stage = normalizeText(step.getStage());
            String studentActivity = normalizeText(step.getStudentActivity());
            String purpose = normalizeText(step.getPurpose());
            if (!StringUtils.hasText(stage) || !StringUtils.hasText(studentActivity)) {
                continue;
            }
            /*
            StringBuilder activity = new StringBuilder(stage).append("：").append(studentActivity);
            if (StringUtils.hasText(purpose)) {
                activity.append("；目标：").append(purpose);
            }
            */
            StringBuilder activity = new StringBuilder(stage).append(": ").append(studentActivity);
            if (StringUtils.hasText(purpose)) {
                activity.append("; purpose: ").append(purpose);
            }
            activities.add(activity.toString());
        }
        return activities;
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private CompletionResult requestCompletion(
            String operation,
            String model,
            String systemPrompt,
            String userPrompt,
            int maxTokens,
            BigDecimal temperature,
            Integer timeoutMs
    ) {
        String baseUrl = requireProperty(properties.getBaseUrl(), "EDU_AI_BASE_URL");
        String apiKey = requireProperty(properties.getApiKey(), "EDU_AI_API_KEY");
        long buildStartedAt = System.nanoTime();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", temperature);
        List<Map<String, String>> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        payload.put("messages", messages);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("max_tokens", maxTokens);

        log.info("{} ai build request cost={} ms, provider={}, model={}, messages={}, systemPromptChars={}, userPromptChars={}, maxTokens={}, temperature={}",
                operation,
                elapsedMillis(buildStartedAt),
                resolveProvider(),
                model,
                messages.size(),
                StringUtils.hasText(systemPrompt) ? systemPrompt.length() : 0,
                userPrompt.length(),
                maxTokens,
                temperature);

        long startedAt = System.nanoTime();
        RestClient currentClient = timeoutMs == null ? restClient : buildTimeoutRestClient(timeoutMs);
        CompletionEnvelope envelope;
        try {
            envelope = currentClient.post()
                    .uri(removeTrailingSlash(baseUrl) + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(payload)
                    .exchange((requestSpec, response) -> {
                        try {
                            return new CompletionEnvelope(
                                    response.getStatusCode().value(),
                                    new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)
                            );
                        } catch (IOException ioException) {
                            throw new IllegalStateException("AI model response read failed", ioException);
                        }
                    });
        } catch (ResourceAccessException exception) {
            if (isTimeoutException(exception)) {
                throw new UserErrorException(HttpStatus.GATEWAY_TIMEOUT, "AI 服务响应超时，请稍后重试。");
            }
            throw new IllegalStateException("AI model request failed, please check model service and configuration", exception);
        } catch (RuntimeException exception) {
            if (isTimeoutException(exception)) {
                throw new UserErrorException(HttpStatus.GATEWAY_TIMEOUT, "AI 服务响应超时，请稍后重试。");
            }
            throw new IllegalStateException("AI model request failed, please check model service and configuration", exception);
        }

        if (envelope == null) {
            throw invalidModelResponse("empty response", null);
        }
        String responseBody = envelope.body() == null ? "" : envelope.body();
        log.info("{} ai http cost={} ms, provider={}, model={}, httpStatus={}, contentLength={}",
                operation, elapsedMillis(startedAt), resolveProvider(), model, envelope.httpStatus(), responseBody.length());
        ensureSuccessfulHttpStatus(envelope.httpStatus());
        if (!StringUtils.hasText(responseBody)) {
            throw invalidModelResponse("empty response", null);
        }

        long parseStartedAt = System.nanoTime();
        try {
            JsonNode completion = objectMapper.readTree(responseBody);
            String content = extractMessageContent(completion);
            JsonNode parsed = parseCompletionContent(content);
            log.info("{} ai parse cost={} ms, provider={}, model={}, httpStatus={}, contentLength={}",
                    operation, elapsedMillis(parseStartedAt), resolveProvider(), model, envelope.httpStatus(), content.length());
            if (log.isDebugEnabled()) {
                log.debug("AI completion content preview: {}", truncate(content, DEBUG_CONTENT_LIMIT));
            }
            return new CompletionResult(envelope.httpStatus(), content, parsed);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.warn("{} ai parse cost={} ms, provider={}, model={}, httpStatus={}, contentLength={}, reason={}",
                    operation, elapsedMillis(parseStartedAt), resolveProvider(), model, envelope.httpStatus(), responseBody.length(), exception.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("AI completion content preview: {}", truncate(responseBody, DEBUG_CONTENT_LIMIT));
            }
            throw invalidModelResponse("invalid JSON", exception);
        }
    }

    private int resolveGradingTimeoutMs() {
        Integer configured = properties.getGradingTimeoutMs();
        if (configured == null || configured <= 0) {
            return DEFAULT_GRADING_TIMEOUT_MS;
        }
        return configured;
    }

    private String resolveGradingModel() {
        if (StringUtils.hasText(properties.getGradingModel())) {
            return properties.getGradingModel().trim();
        }
        return resolveModelFallback();
    }

    private String resolveLessonModel() {
        if (StringUtils.hasText(properties.getLessonModel())) {
            return properties.getLessonModel().trim();
        }
        return resolveModelFallback();
    }

    private String resolveModelFallback() {
        return requireProperty(properties.getModel(), "EDU_AI_MODEL");
    }

    private RestClient buildTimeoutRestClient(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(timeoutMs);
        return restClientBuilder.requestFactory(factory).build();
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void requireNonEmptyListV2(List<?> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw invalidModelResponse(fieldName + " missing", null);
        }
    }

    private void requirePresentListV2(List<?> values, String fieldName) {
        if (values == null) {
            throw invalidModelResponse(fieldName + " missing", null);
        }
    }

    private void requireAtMostTwoItems(List<?> values, String fieldName) {
        if (values != null && values.size() > MAX_GRADING_FEEDBACK_ITEMS) {
            throw invalidModelResponse(fieldName + " exceeds limit", null);
        }
    }

    private record CachedGradingResponse(Instant cachedAt, GradingGenerateResponse response) {
    }

    private record CachedLessonPlanResponse(Instant cachedAt, LessonPlanGenerateResponse response) {
    }

    private CompletionResult requestCompletion(String systemPrompt, String userPrompt, int maxTokens, BigDecimal temperature) {
        String baseUrl = requireProperty(properties.getBaseUrl(), "EDU_AI_BASE_URL");
        String apiKey = requireProperty(properties.getApiKey(), "EDU_AI_API_KEY");
        String model = requireProperty(properties.getModel(), "EDU_AI_MODEL");
        long buildStartedAt = System.nanoTime();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", temperature);
        List<Map<String, String>> messages = new java.util.ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        payload.put("messages", messages);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("max_tokens", maxTokens);

        String operation = maxTokens == GRADING_MAX_TOKENS ? GRADING_OPERATION
                : maxTokens == LESSON_PLAN_MAX_TOKENS ? LESSON_PLAN_OPERATION : "ai";
        log.info("{} ai build request cost={} ms, provider={}, model={}, messages={}, systemPromptChars={}, userPromptChars={}, maxTokens={}, temperature={}",
                operation,
                elapsedMillis(buildStartedAt),
                resolveProvider(),
                model,
                messages.size(),
                StringUtils.hasText(systemPrompt) ? systemPrompt.length() : 0,
                userPrompt.length(),
                maxTokens,
                temperature);

        long startedAt = System.nanoTime();
        CompletionEnvelope envelope;
        try {
            envelope = restClient.post()
                    .uri(removeTrailingSlash(baseUrl) + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(payload)
                    .exchange((request, response) -> {
                        try {
                            return new CompletionEnvelope(
                                    response.getStatusCode().value(),
                                    new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)
                            );
                        } catch (IOException ioException) {
                            throw new IllegalStateException("AI model response read failed", ioException);
                        }
                    });
        } catch (RuntimeException exception) {
            throw new IllegalStateException("AI model request failed, please check model service and configuration", exception);
        }

        if (envelope == null) {
            throw invalidModelResponse("empty response", null);
        }
        String responseBody = envelope.body() == null ? "" : envelope.body();
        log.info("{} ai http cost={} ms, provider={}, model={}, httpStatus={}, contentLength={}",
                operation, elapsedMillis(startedAt), resolveProvider(), model, envelope.httpStatus(), responseBody.length());
        ensureSuccessfulHttpStatus(envelope.httpStatus());
        if (!StringUtils.hasText(responseBody)) {
            throw invalidModelResponse("empty response", null);
        }
        long parseStartedAt = System.nanoTime();
        try {
            JsonNode completion = objectMapper.readTree(responseBody);
            String content = extractMessageContent(completion);
            JsonNode parsed = parseCompletionContent(content);
            log.info("{} ai parse cost={} ms, provider={}, model={}, httpStatus={}, contentLength={}",
                    operation, elapsedMillis(parseStartedAt), resolveProvider(), model, envelope.httpStatus(), content.length());
            if (log.isDebugEnabled()) {
                log.debug("AI completion content preview: {}", truncate(content, DEBUG_CONTENT_LIMIT));
            }
            return new CompletionResult(envelope.httpStatus(), content, parsed);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.warn("{} ai parse cost={} ms, provider={}, model={}, httpStatus={}, contentLength={}, reason={}",
                    operation, elapsedMillis(parseStartedAt), resolveProvider(), model, envelope.httpStatus(), responseBody.length(), exception.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("AI completion content preview: {}", truncate(responseBody, DEBUG_CONTENT_LIMIT));
            }
            throw invalidModelResponse("invalid JSON", exception);
        }
    }

    private void ensureSuccessfulHttpStatus(int httpStatus) {
        if (httpStatus >= 200 && httpStatus < 300) {
            return;
        }
        throw switch (httpStatus) {
            case 401, 403 -> new UserErrorException(HttpStatus.UNAUTHORIZED, "AI 服务认证或权限异常，请联系管理员。");
            case 429 -> new UserErrorException(HttpStatus.TOO_MANY_REQUESTS, "AI 服务请求繁忙，请稍后重试。");
            case 500, 502, 503, 504 -> new UserErrorException(HttpStatus.SERVICE_UNAVAILABLE, "AI 服务暂时不可用，请稍后重试。");
            default -> new UserErrorException(HttpStatus.BAD_GATEWAY, "AI 服务请求失败，请稍后重试。");
        };
    }

    private String extractMessageContent(JsonNode completion) {
        JsonNode content = completion.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder text = new StringBuilder();
            for (JsonNode part : content) {
                if (part.path("text").isTextual()) {
                    text.append(part.path("text").asText());
                }
            }
            if (text.length() > 0) return text.toString();
        }
        throw new IllegalArgumentException("choices.message.content 缺失");
    }

    private String stripJsonCodeFence(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.startsWith("```")) {
            int firstLineBreak = normalized.indexOf('\n');
            if (firstLineBreak < 0) throw new IllegalArgumentException("代码围栏内容为空");
            normalized = normalized.substring(firstLineBreak + 1).trim();
            if (normalized.endsWith("```")) {
                normalized = normalized.substring(0, normalized.length() - 3).trim();
            }
        }
        return normalized;
    }

    private JsonNode parseCompletionContent(String content) throws JsonProcessingException {
        String normalized = stripJsonCodeFence(content);
        JsonNode direct = tryParseJson(normalized);
        if (direct != null) {
            if (direct.isTextual() && StringUtils.hasText(direct.asText())) {
                return parseCompletionContent(direct.asText());
            }
            return direct;
        }

        String extracted = extractOuterJsonObject(normalized);
        if (extracted == null) {
            throw new IllegalArgumentException("未找到完整 JSON 对象");
        }
        JsonNode parsed = tryParseJson(extracted);
        if (parsed == null) {
            throw new IllegalArgumentException("完整 JSON 对象无法解析");
        }
        if (parsed.isTextual() && StringUtils.hasText(parsed.asText())) {
            return parseCompletionContent(parsed.asText());
        }
        return parsed;
    }

    private JsonNode tryParseJson(String value) throws JsonProcessingException {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return objectMapper.readTree(value.trim());
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String extractOuterJsonObject(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        int start = text.indexOf('{');
        while (start >= 0) {
            int depth = 0;
            boolean inString = false;
            boolean escaped = false;
            for (int index = start; index < text.length(); index++) {
                char current = text.charAt(index);
                if (inString) {
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == '"') {
                        inString = false;
                    }
                    continue;
                }
                if (current == '"') {
                    inString = true;
                    continue;
                }
                if (current == '{') {
                    depth++;
                } else if (current == '}') {
                    depth--;
                    if (depth == 0) {
                        return text.substring(start, index + 1);
                    }
                }
            }
            start = text.indexOf('{', start + 1);
        }
        return null;
    }

    void validateGradingResponse(GradingGenerateRequest request, GradingGenerateResponse response) {
        if (response == null || response.getTotalScore() == null) {
            throw invalidModelResponse("批改总分缺失", null);
        }
        if (!StringUtils.hasText(response.getReferenceAnswer())
                || !StringUtils.hasText(response.getRevisedAnswer())) {
            throw invalidModelResponse("参考答案或改写答案缺失", null);
        }
        requireNonEmptyList(response.getDimensionScores(), "分项评分");
        requireNonEmptyList(response.getStrengths(), "答案优点");
        requireAtMostThreeItems(response.getStrengths(), "答案优点");
        requirePresentList(response.getDeductions(), "扣分原因");
        requireAtMostThreeItems(response.getDeductions(), "扣分原因");
        requirePresentList(response.getSuggestions(), "修改建议");
        requireAtMostThreeItems(response.getSuggestions(), "修改建议");
        if (response.getDimensionScores().size() != request.getRubric().size()) {
            throw invalidModelResponse("分项评分数量与 Rubric 不一致", null);
        }

        BigDecimal dimensionTotal = ZERO;
        for (int index = 0; index < request.getRubric().size(); index++) {
            AiRubricItem expected = request.getRubric().get(index);
            GradingDimensionScore actual = response.getDimensionScores().get(index);
            if (actual == null
                    || !expected.getCriterion().trim().equals(actual.getCriterion())
                    || actual.getMaxScore() == null
                    || actual.getMaxScore().compareTo(expected.getMaxScore()) != 0
                    || actual.getScore() == null
                    || !StringUtils.hasText(actual.getReason())) {
                throw invalidModelResponse("分项评分未与请求 Rubric 一一对应", null);
            }
            if (actual.getScore().compareTo(ZERO) < 0
                    || actual.getScore().compareTo(actual.getMaxScore()) > 0
                    || !hasAtMostOneDecimal(actual.getScore())) {
                throw invalidModelResponse("分项分数超出范围或精度不正确", null);
            }
            dimensionTotal = dimensionTotal.add(actual.getScore());
        }
        if (response.getTotalScore().compareTo(ZERO) < 0
                || response.getTotalScore().compareTo(request.getMaxScore()) > 0
                || !hasAtMostOneDecimal(response.getTotalScore())
                || dimensionTotal.compareTo(response.getTotalScore()) != 0) {
            throw invalidModelResponse("批改总分与分项分数不一致", null);
        }
        if (response.getConfidence() == null
                || response.getConfidence().compareTo(ZERO) < 0
                || response.getConfidence().compareTo(ONE) > 0) {
            throw invalidModelResponse("置信度超出范围", null);
        }
    }

    private void requireNonEmptyList(List<?> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw invalidModelResponse(fieldName + "缺失", null);
        }
    }

    private void requirePresentList(List<?> values, String fieldName) {
        if (values == null) {
            throw invalidModelResponse(fieldName + "缺失", null);
        }
    }

    private void requireAtMostThreeItems(List<?> values, String fieldName) {
        if (values != null && values.size() > 3) {
            throw invalidModelResponse(fieldName + "超过 3 条", null);
        }
    }

    private boolean hasAtMostOneDecimal(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 1;
    }

    private String requireProperty(String value, String envName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(envName + " 未配置，无法调用真实模型");
        }
        return value.trim();
    }

    private String removeTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI请求内容无法序列化", exception);
        }
    }

    private GradingGenerateResponse generateGradingWithRetry(GradingGenerateRequest request) {
        long startedAt = System.nanoTime();
        try {
            CompletionResult first = requestCompletion(gradingSystemPrompt(), buildGradingPrompt(request), GRADING_MAX_TOKENS, GRADING_TEMPERATURE);
            return parseGradingResponse(request, first);
        } catch (IllegalStateException firstFailure) {
            if (!shouldRetryGradingFailure(firstFailure)) {
                throw firstFailure;
            }
            CompletionResult retryResult;
            try {
                retryResult = requestCompletion(gradingSystemPrompt(), buildGradingRepairPrompt(request), GRADING_MAX_TOKENS, GRADING_TEMPERATURE);
            } catch (IllegalStateException secondRequestFailure) {
                throw secondRequestFailure;
            }
            return parseGradingResponse(request, retryResult);
        } finally {
            log.info("grading ai total cost={} ms, provider={}, model={}",
                    elapsedMillis(startedAt), resolveProvider(), resolveModel());
        }
    }

    private boolean shouldRetryGradingFailure(IllegalStateException exception) {
        String message = exception.getMessage();
        return StringUtils.hasText(message) && message.startsWith("AI模型返回结果无效");
    }

    private GradingGenerateResponse parseGradingResponse(
            GradingGenerateRequest request,
            CompletionResult result
    ) {
        try {
            GradingGenerateResponse response = objectMapper.treeToValue(result.content(), GradingGenerateResponse.class);
            validateGradingResponse(request, response);
            response.setReferenceAnswer(request.getReferenceAnswer());
            response.setProvider(resolveProvider());
            response.setModel(resolveModel());
            return response;
        } catch (JsonProcessingException exception) {
            log.warn("AI grading invalid, provider={}, model={}, httpStatus={}, contentLength={}, reason={}",
                    resolveProvider(), resolveModel(), result.httpStatus(), result.rawContent().length(), exception.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("AI grading content preview: {}", truncate(result.rawContent(), DEBUG_CONTENT_LIMIT));
            }
            throw invalidModelResponse("批改 JSON 结构无法解析", exception);
        } catch (IllegalStateException exception) {
            log.warn("AI grading invalid, provider={}, model={}, httpStatus={}, contentLength={}, reason={}",
                    resolveProvider(), resolveModel(), result.httpStatus(), result.rawContent().length(), exception.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("AI grading content preview: {}", truncate(result.rawContent(), DEBUG_CONTENT_LIMIT));
            }
            throw exception;
        }
    }

    private String resolveProvider() {
        return StringUtils.hasText(properties.getProvider())
                ? properties.getProvider().trim()
                : "openai-compatible";
    }

    private String resolveModel() {
        return requireProperty(properties.getModel(), "EDU_AI_MODEL");
    }

    private String gradingJsonSchema() {
        return """
                {
                  "totalScore": 0,
                  "dimensionScores": [
                    {
                      "criterion": "",
                      "score": 0,
                      "maxScore": 0,
                      "reason": ""
                    }
                  ],
                  "strengths": [],
                  "deductions": [],
                  "suggestions": [],
                  "referenceAnswer": "",
                  "revisedAnswer": "",
                  "confidence": 0
                }
                """;
    }

    private String truncate(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "…";
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private IllegalStateException invalidModelResponse(String message, Throwable cause) {
        return cause == null ? new IllegalStateException("AI模型返回结果无效：" + message)
                : new IllegalStateException("AI模型返回结果无效：" + message, cause);
    }

    private record CompletionEnvelope(int httpStatus, String body) {
    }

    private record CompletionResult(int httpStatus, String rawContent, JsonNode content) {
    }
}
