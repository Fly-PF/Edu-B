package com.edu.ai.client;

import com.edu.common.properties.AIModelProperties;
import com.edu.exception.UserErrorException;
import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingDimensionScore;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanExercise;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanTeachingStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "edu.ai-model.teacher-ai.chat-model.supplier", havingValue = "compatible", matchIfMissing = true)
public class OpenAiCompatibleAiModelClient implements AiModelClient {
    private final AIModelProperties aiModelProperties;
    private final ObjectMapper objectMapper;
    private static final int DEFAULT_MAX_TOKENS = 560;

    @Override
    public LessonPlanGenerateResponse generateLessonPlan(LessonPlanGenerateRequest request) {
        String system = "你是中小学教师的智能备课助手。请使用简体中文，只返回一个 JSON 对象，不要返回 Markdown。"
                + "字段必须为 title、objectives、keyPoints、difficultPoints、preparations、teachingSteps、"
                + "activities、exercises、rubric、notes。teachingSteps 的元素字段为 stage、durationMinutes、"
                + "teacherActivity、studentActivity、purpose，所有步骤时长之和必须等于课时。"
                + "exercises 的元素字段为 question、type、referenceAnswer、difficulty。"
                + "rubric 的元素字段为 criterion、description、maxScore。";
        String user = "课题：" + request.getTopic()
                + "\n学段：" + request.getGrade()
                + "\n课时：" + request.getDurationMinutes() + "分钟"
                + "\n教学目标：" + request.getObjectives()
                + "\n难度：" + request.getDifficulty()
                + "\n补充要求：" + safe(request.getRequirements());
        JsonNode json = requestJson(system, user, 2200);
        try {
            LessonPlanGenerateResponse response = objectMapper.treeToValue(json, LessonPlanGenerateResponse.class);
            normalizeLessonPlan(request, response);
            return response;
        } catch (Exception exception) {
            throw invalidModelResponse("教案 JSON 解析失败", exception);
        }
    }

    @Override
    public GradingGenerateResponse generateGrading(GradingGenerateRequest request) {
        String system = "你是教师的开放题辅助批改助手。必须保守评分，不能把语言流畅等同于知识正确。"
                + "只能把学生答案中明确写出的内容作为得分证据，严禁把题目、参考答案或评分标准中的内容说成学生已经回答。"
                + "每个分项评分理由必须忠实引用或概括学生答案中的实际内容；找不到对应证据时该项必须给0分。"
                + "评分时题目的明确要求优先于参考答案。参考答案可能只是示例集合；若题目要求‘至少N个’，"
                + "学生已正确写出N个并完成要求，就不得因未覆盖参考答案中的其他示例而扣分。"
                + "各评分维度独立评价，知识点缺失只能在知识或完整性维度扣分，不能在逻辑表达维度重复扣分。"
                + "如果学生答案是随机字符、无意义内容、答非所问或没有回答题目要求，所有分项均给0分，"
                + "strengths 写明暂无有效得分点，deductions 明确指出答案无效。"
                + "请使用简体中文，只返回一个 JSON 对象，不要返回 Markdown。字段必须为 totalScore、"
                + "dimensionScores、strengths、deductions、suggestions、revisedAnswer、confidence。"
                + "dimensionScores 的元素字段为 criterion、score、maxScore、reason。"
                + "每项得分不能超过该项满分，总分必须等于各项得分之和；confidence 为 0 到 1。";
        String user = "【题目】\n" + request.getQuestion()
                + "\n题型：" + request.getQuestionType()
                + "\n【参考答案，仅用于对照，不代表学生已回答】\n" + request.getReferenceAnswer()
                + "\n【学生答案，唯一得分证据】\n" + request.getStudentAnswer()
                + "\n题目满分：" + request.getMaxScore()
                + "\n【评分标准】\n" + rubricText(request.getRubric())
                + "\n输出前再次检查：评分理由中声称学生回答的每个知识点，必须能在学生答案原文中找到。";
        JsonNode json = requestJson(system, user, 1400, teacherModel().getModelName());
        return normalizeGrading(request, json);
    }

    private JsonNode requestJson(String system, String user, int maxTokens) {
        return requestJson(system, user, maxTokens, teacherModel().getModelName());
    }

    private JsonNode requestJson(String system, String user, int maxTokens, String model) {
        if (!aiModelProperties.getTeacherAi().isEnabled() || !StringUtils.hasText(teacherModel().getBaseUrl())) {
            throw new UserErrorException(HttpStatus.SERVICE_UNAVAILABLE, "AI 模型尚未配置，请检查云端 API 配置");
        }
        try {
            HttpResponse<String> response = send(system, user, maxTokens, true, model);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response = send(system, user, maxTokens, false, model);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("模型接口返回 HTTP " + response.statusCode());
            }
            String content = extractContent(objectMapper.readTree(response.body()));
            return objectMapper.readTree(extractJson(content));
        } catch (UserErrorException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("教师 AI 模型调用失败", exception);
            throw new UserErrorException(HttpStatus.SERVICE_UNAVAILABLE, "AI 模型暂时不可用，请检查云端 API 配置");
        }
    }

    private HttpResponse<String> send(String system, String user, int maxTokens, boolean jsonMode, String model) throws Exception {
        var payload = objectMapper.createObjectNode();
        payload.put("model", StringUtils.hasText(model) ? model : teacherModel().getModelName());
        payload.put("temperature", 0.15);
        payload.put("max_tokens", Math.max(maxTokens, teacherModel().getMaxTokens() == null ? DEFAULT_MAX_TOKENS : teacherModel().getMaxTokens()));
        if (jsonMode) {
            payload.putObject("response_format").put("type", "json_object");
        }
        var messages = payload.putArray("messages");
        messages.addObject().put("role", "system").put("content", system);
        messages.addObject().put("role", "user").put("content", user);

        AIModelProperties.Model config = teacherModel();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.getBaseUrl()))
                .timeout(Duration.ofMillis(Math.max(15_000, config.getTimeout() == null ? 90_000 : config.getTimeout())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
        if (StringUtils.hasText(config.getApiKey())) {
            builder.header("Authorization", "Bearer " + config.getApiKey());
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(10_000, config.getTimeout() == null ? 90_000 : config.getTimeout())))
                .build();
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private AIModelProperties.Model teacherModel() {
        return aiModelProperties.getTeacherAi().getChatModel();
    }

    private GradingGenerateResponse normalizeGrading(GradingGenerateRequest request, JsonNode json) {
        JsonNode rawDimensions = json.path("dimensionScores");
        List<GradingDimensionScore> dimensions = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < request.getRubric().size(); index++) {
            AiRubricItem rubric = request.getRubric().get(index);
            JsonNode raw = rawDimensions.isArray() && index < rawDimensions.size()
                    ? rawDimensions.get(index)
                    : objectMapper.createObjectNode();
            BigDecimal score = normalizeScore(decimal(raw.path("score")), rubric.getMaxScore());
            total = total.add(score);
            dimensions.add(GradingDimensionScore.builder()
                    .criterion(rubric.getCriterion())
                    .score(score)
                    .maxScore(rubric.getMaxScore())
                    .reason(textOr(raw.path("reason"), "请教师结合学生答案复核本项得分。"))
                    .build());
        }
        if (dimensions.isEmpty()) {
            throw invalidModelResponse("模型没有返回分项评分", null);
        }

        List<String> strengths = textList(json.path("strengths"));
        List<String> deductions = textList(json.path("deductions"));
        List<String> suggestions = textList(json.path("suggestions"));
        return GradingGenerateResponse.builder()
                .totalScore(total)
                .dimensionScores(dimensions)
                .strengths(strengths.isEmpty() ? List.of("已完成本题作答，请教师结合评分标准复核。") : strengths)
                .deductions(deductions.isEmpty() ? List.of("暂未识别出明确扣分点，请教师复核答案细节。") : deductions)
                .suggestions(suggestions.isEmpty() ? List.of("建议对照参考答案补充关键概念和推理过程。") : suggestions)
                .revisedAnswer(textOr(json.path("revisedAnswer"), request.getReferenceAnswer()))
                .confidence(normalizeConfidence(decimal(json.path("confidence"))))
                .build();
    }

    private void normalizeLessonPlan(LessonPlanGenerateRequest request, LessonPlanGenerateResponse response) {
        if (!StringUtils.hasText(response.getTitle())) {
            response.setTitle(request.getGrade() + "《" + request.getTopic() + "》教学设计");
        }
        if (response.getTeachingSteps() == null || response.getTeachingSteps().isEmpty()) {
            throw invalidModelResponse("模型没有返回教学步骤", null);
        }
        int total = response.getTeachingSteps().stream()
                .mapToInt(step -> step == null || step.getDurationMinutes() == null ? 0 : step.getDurationMinutes())
                .sum();
        int difference = request.getDurationMinutes() - total;
        LessonPlanTeachingStep last = response.getTeachingSteps().getLast();
        int adjusted = (last.getDurationMinutes() == null ? 0 : last.getDurationMinutes()) + difference;
        if (adjusted <= 0) {
            throw invalidModelResponse("教学步骤时长无效", null);
        }
        last.setDurationMinutes(adjusted);
        if (response.getExercises() == null || response.getExercises().isEmpty()) {
            response.setExercises(List.of(LessonPlanExercise.builder()
                    .question("请用自己的话总结“" + request.getTopic() + "”的核心内容。")
                    .type("简答题")
                    .referenceAnswer(request.getObjectives())
                    .difficulty(request.getDifficulty())
                    .build()));
        }
        if (response.getRubric() == null || response.getRubric().isEmpty()) {
            response.setRubric(List.of(AiRubricItem.builder()
                    .criterion("目标达成")
                    .description("能够准确理解并应用本课核心内容")
                    .maxScore(BigDecimal.valueOf(100))
                    .build()));
        }
    }

    private String rubricText(List<AiRubricItem> rubric) {
        return rubric.stream()
                .map(item -> item.getCriterion() + "（" + item.getMaxScore() + "分）：" + item.getDescription())
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
    }

    private String extractContent(JsonNode response) {
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isTextual() && StringUtils.hasText(content.asText())) {
            return content.asText().trim();
        }
        throw invalidModelResponse("模型没有返回有效内容", null);
    }

    private String extractJson(String content) {
        String value = content == null ? "" : content.trim();
        if (value.startsWith("```")) {
            int firstLine = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                value = value.substring(firstLine + 1, lastFence).trim();
            }
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw invalidModelResponse("模型返回内容不是 JSON", null);
        }
        return value.substring(start, end + 1);
    }

    private List<String> textList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> {
                String value = item.asText().trim();
                if (StringUtils.hasText(value)) values.add(value);
            });
        }
        return values;
    }

    private BigDecimal decimal(JsonNode node) {
        try {
            return node.isNumber() ? node.decimalValue() : new BigDecimal(node.asText());
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal normalizeScore(BigDecimal score, BigDecimal maxScore) {
        return score.max(BigDecimal.ZERO).min(maxScore).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private BigDecimal normalizeConfidence(BigDecimal confidence) {
        BigDecimal fallback = confidence.compareTo(BigDecimal.ZERO) == 0 ? new BigDecimal("0.70") : confidence;
        return fallback.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }

    private String textOr(JsonNode node, String fallback) {
        String value = node == null ? "" : node.asText().trim();
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "无";
    }

    private IllegalStateException invalidModelResponse(String detail, Exception cause) {
        return cause == null
                ? new IllegalStateException("AI 返回结果不符合约束：" + detail)
                : new IllegalStateException("AI 返回结果不符合约束：" + detail, cause);
    }
}
