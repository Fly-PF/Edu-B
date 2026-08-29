package com.edu.learninganalysis;

import com.edu.common.properties.AIModelProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** OpenAI-compatible, course-scoped generator used by the learning-growth loop. */
@Slf4j
@Component
public class CourseAssistantGateway {
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final boolean jsonMode;

    public CourseAssistantGateway(ObjectMapper objectMapper, AIModelProperties properties) {
        AIModelProperties.Model config = properties.getLearningAnalysis().getChatModel();
        this.objectMapper = objectMapper;
        this.baseUrl = normalizeBaseUrl(config.getBaseUrl());
        this.apiKey = config.getApiKey();
        this.model = StringUtils.hasText(config.getModelName()) ? config.getModelName() : "gpt-4.1-mini";
        this.jsonMode = true;
    }

    public String modelName() {
        return model;
    }

    public Optional<PlanResponse> generatePlan(PlanRequest input) {
        String system = "你是课程内学习诊断助手。只能依据给定课程上下文和真实学习行为生成计划；"
                + "不编造教材内容、学习记录或成绩。计划必须是学生在15到45分钟内可执行的微任务。"
                + "只返回JSON对象，不要markdown。字段必须为：diagnosis、goal、taskSteps、durationMinutes、"
                + "acceptanceCriteria、checkQuestion、expectedSignals。taskSteps和expectedSignals是字符串数组。";
        String user = "课程上下文：\n" + input.courseContext()
                + "\n\n行为风险：\n" + input.riskSummary()
                + "\n\n优先章节：" + input.nextChapter();
        return requestJson(system, user).flatMap(this::toPlanResponse);
    }

    public Optional<EvidenceResponse> assessEvidence(EvidenceRequest input) {
        String system = "你是课程内理解检查助手。只能基于给定课程上下文、问题和学生回答进行保守判断，"
                + "不能把语言流畅当成已掌握。只返回JSON对象，不要markdown。字段必须为："
                + "result（MASTERED、RETRY、TEACHER_REVIEW之一）、assessment、confidence（0到100整数）。";
        String user = "课程上下文：\n" + input.courseContext()
                + "\n\n理解检查问题：" + input.question()
                + "\n\n学生回答：" + input.answer();
        return requestJson(system, user).flatMap(this::toEvidenceResponse);
    }

    /**
     * The model can rank only the persisted candidate courses supplied by the server.
     * This prevents an attractive but nonexistent course from appearing in a recommendation.
     */
    public Optional<List<CourseRecommendationResponse>> recommendCourses(RecommendationRequest input) {
        if (input.candidates() == null || input.candidates().isEmpty()) {
            return Optional.of(List.of());
        }
        String system = "你是学习路径推荐助手。只能从服务端给出的候选课程中选择，不得编造课程、课程ID、"
                + "学习记录或能力结论。根据学生真实课程主题占比、已学课程和候选课程简介，给出最多3门下一步课程。"
                + "优先说明它与学生当前学习主题的衔接，或为什么能补足单一主题。"
                + "只返回JSON对象，不要markdown。字段必须为recommendations，数组元素字段为courseId、reason、score（0到100整数）。";
        String candidates = input.candidates().stream()
                .map(item -> "ID=" + item.courseId() + "；名称=" + item.courseName()
                        + "；主题=" + item.courseCategory() + "；难度=" + item.difficulty() + "；简介=" + item.intro())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        String user = "学生学习画像：\n" + input.profileSummary() + "\n\n可推荐课程（只能从中选择）：\n" + candidates;
        return requestJson(system, user).flatMap(node -> toRecommendationResponses(node, input.candidates()));
    }

    /** Answers only from the server-supplied learning facts, never from invented grades or records. */
    public Optional<LearningQuestionResponse> answerLearningQuestion(LearningQuestionRequest input) {
        String system = "你是校园学习分析助理，不是通用问答机器人。只能依据服务端给出的真实学习记录、能力画像、课程主题、"
                + "风险证据和趋势回答，不得编造成绩、知识点、学习时长、课程或学生行为。"
                + "先给出判断，再指出支持判断的具体数据，最后给出一项在本周可完成的下一步。"
                + "answer 用 2 到 4 句自然语言直答问题；nextStep 只写一个明确动作；recommendedChapter 只能填写上下文中已有章节，"
                + "没有对应章节时留空；references 必须引用 1 到 3 条上下文中已有的事实。"
                + "只返回 JSON 对象，不要 markdown。字段必须为 answer、nextStep、recommendedChapter、references；"
                + "references 是字符串数组。";
        String user = input.audience() + "视角的真实学情：\n" + input.learningContext()
                + "\n\n问题：" + input.question();
        return requestJson(system, user).flatMap(this::toLearningQuestionResponse);
    }

    private Optional<JsonNode> requestJson(String system, String user) {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey)) {
            log.debug("Learning AI provider is not configured; set edu.ai-model.learning-analysis.chat-model base-url and api-key");
            return Optional.empty();
        }
        Optional<String> content = requestCompletion(system, user, jsonMode);
        if (content.isEmpty() && jsonMode) {
            log.info("Learning AI JSON mode was unavailable; retrying without response_format");
            content = requestCompletion(system, user, false);
        }
        return content.flatMap(this::parseJsonContent);
    }

    private Optional<String> requestCompletion(String system, String user, boolean requireJson) {
        try {
            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .model(model)
                    .temperature(0.2);
            if (requireJson) {
                optionsBuilder.responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build());
            }
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .options(optionsBuilder.build())
                    .observationRegistry(ObservationRegistry.NOOP)
                    .build();
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(system),
                    new UserMessage(user)
            )));
            String content = response == null || response.getResult() == null
                    || response.getResult().getOutput() == null
                    ? ""
                    : response.getResult().getOutput().getText();
            if (!StringUtils.hasText(content)) {
                log.warn("Learning AI provider returned no assistant message content");
                return Optional.empty();
            }
            return Optional.of(content.trim());
        } catch (Exception exception) {
            log.warn("Learning AI provider unavailable: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<JsonNode> parseJsonContent(String content) {
        String cleaned = stripCodeFence(content);
        try {
            return Optional.of(objectMapper.readTree(cleaned));
        } catch (Exception firstError) {
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return Optional.of(objectMapper.readTree(cleaned.substring(start, end + 1)));
                } catch (Exception ignored) {
                    // Log below with the original parsing failure.
                }
            }
            log.warn("Learning AI provider returned invalid JSON: {}", abbreviate(cleaned, 500));
            return Optional.empty();
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        return value.replaceFirst("/chat/completions/?$", "");
    }

    private String abbreviate(String value, int maxLength) {
        String safe = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength) + "...";
    }

    private Optional<PlanResponse> toPlanResponse(JsonNode node) {
        List<String> steps = stringList(node.path("taskSteps"));
        List<String> signals = stringList(node.path("expectedSignals"));
        PlanResponse result = new PlanResponse(
                node.path("diagnosis").asText().trim(),
                node.path("goal").asText().trim(),
                steps,
                node.path("durationMinutes").asInt(),
                node.path("acceptanceCriteria").asText().trim(),
                node.path("checkQuestion").asText().trim(),
                signals,
                model
        );
        return result.hasRequiredFields() ? Optional.of(result) : Optional.empty();
    }

    private Optional<EvidenceResponse> toEvidenceResponse(JsonNode node) {
        String result = node.path("result").asText().trim();
        int confidence = node.path("confidence").asInt(-1);
        EvidenceResponse response = new EvidenceResponse(
                result,
                node.path("assessment").asText().trim(),
                confidence,
                model
        );
        return response.isValid() ? Optional.of(response) : Optional.empty();
    }

    private Optional<List<CourseRecommendationResponse>> toRecommendationResponses(
            JsonNode node,
            List<CourseCandidate> candidates
    ) {
        Map<Long, CourseCandidate> candidateById = candidates.stream()
                .collect(java.util.stream.Collectors.toMap(CourseCandidate::courseId, item -> item, (left, right) -> left));
        JsonNode items = node.path("recommendations");
        if (!items.isArray()) {
            return Optional.empty();
        }
        List<CourseRecommendationResponse> result = new ArrayList<>();
        Set<Long> selected = new HashSet<>();
        items.forEach(item -> {
            long courseId = item.path("courseId").asLong(0);
            String reason = item.path("reason").asText().trim();
            int score = item.path("score").asInt(-1);
            if (candidateById.containsKey(courseId) && selected.add(courseId)
                    && StringUtils.hasText(reason) && score >= 0 && score <= 100 && result.size() < 3) {
                result.add(new CourseRecommendationResponse(courseId, reason, score, model));
            }
        });
        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }

    private Optional<LearningQuestionResponse> toLearningQuestionResponse(JsonNode node) {
        String answer = node.path("answer").asText().trim();
        String nextStep = node.path("nextStep").asText().trim();
        String chapter = node.path("recommendedChapter").asText().trim();
        List<String> references = stringList(node.path("references")).stream().limit(3).toList();
        LearningQuestionResponse response = new LearningQuestionResponse(answer, nextStep, chapter, references);
        return response.isValid() ? Optional.of(response) : Optional.empty();
    }

    private List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> {
                String value = item.asText().trim();
                if (StringUtils.hasText(value)) {
                    values.add(value);
                }
            });
        }
        return values;
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    public record PlanRequest(String courseContext, String riskSummary, String nextChapter) {
    }

    public record PlanResponse(
            String diagnosis,
            String goal,
            List<String> taskSteps,
            int durationMinutes,
            String acceptanceCriteria,
            String checkQuestion,
            List<String> expectedSignals,
            String modelName
    ) {
        boolean hasRequiredFields() {
            return StringUtils.hasText(diagnosis) && StringUtils.hasText(goal)
                    && taskSteps.size() >= 2 && durationMinutes >= 10 && durationMinutes <= 60
                    && StringUtils.hasText(acceptanceCriteria) && StringUtils.hasText(checkQuestion)
                    && !expectedSignals.isEmpty();
        }
    }

    public record EvidenceRequest(String courseContext, String question, String answer) {
    }

    public record EvidenceResponse(String result, String assessment, int confidence, String modelName) {
        boolean isValid() {
            return List.of("MASTERED", "RETRY", "TEACHER_REVIEW").contains(result)
                    && StringUtils.hasText(assessment) && confidence >= 0 && confidence <= 100;
        }
    }

    public record CourseCandidate(Long courseId, String courseName, String courseCategory, Integer difficulty, String intro) {
    }

    public record RecommendationRequest(String profileSummary, List<CourseCandidate> candidates) {
    }

    public record CourseRecommendationResponse(Long courseId, String reason, int score, String modelName) {
    }

    public record LearningQuestionRequest(String audience, String learningContext, String question) {
    }

    public record LearningQuestionResponse(String answer, String nextStep, String recommendedChapter, List<String> references) {
        boolean isValid() {
            return StringUtils.hasText(answer) && StringUtils.hasText(nextStep) && references != null && !references.isEmpty();
        }
    }
}
