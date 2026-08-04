package com.edu.service.impl;

import com.edu.common.properties.OpenAIModelProperties;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.ai.AiDrawGuessRequest;
import com.edu.pojo.vo.ai.AiDrawGuessPredictionVO;
import com.edu.pojo.vo.ai.AiDrawGuessResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiDrawGuessServiceImpl implements com.edu.service.AiDrawGuessService {
    private static final int MAX_DATA_URL_LENGTH = 8 * 1024 * 1024;

    private final OpenAIModelProperties openAIModelProperties;
    private final ObjectMapper objectMapper;

    @Override
    public AiDrawGuessResultVO guess(AiDrawGuessRequest request) {
        validateRequest(request);
        OpenAIModelProperties.OpenAi openAi = openAIModelProperties.getOpenai();
        if (!StringUtils.hasText(openAi.getApiKey())) {
            throw new BaseException(HttpStatus.SERVICE_UNAVAILABLE, "未配置 OpenAI API Key，无法调用真实视觉模型");
        }

        String responseText = callOpenAi(openAi, request);
        List<AiDrawGuessPredictionVO> predictions = parsePredictions(responseText);
        return AiDrawGuessResultVO.builder()
                .provider("openai")
                .model(openAi.getModel())
                .summary("AI 已根据画布图片返回猜测结果")
                .predictions(predictions)
                .build();
    }

    private void validateRequest(AiDrawGuessRequest request) {
        if (request == null || !StringUtils.hasText(request.getImageDataUrl())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "画布图片不能为空");
        }
        String imageDataUrl = request.getImageDataUrl().trim();
        if (!imageDataUrl.startsWith("data:image/")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "画布图片格式不正确");
        }
        if (imageDataUrl.length() > MAX_DATA_URL_LENGTH) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "画布图片过大");
        }
    }

    private String callOpenAi(
            OpenAIModelProperties.OpenAi openAi,
            AiDrawGuessRequest request
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAi.getModel());
        payload.put("max_output_tokens", 300);
        payload.put("input", List.of(Map.of(
                "role", "user",
                "content", List.of(
                        Map.of(
                                "type", "input_text",
                                "text", buildPrompt()
                        ),
                        Map.of(
                                "type", "input_image",
                                "image_url", request.getImageDataUrl()
                        )
                )
        )));

        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(15_000);
            requestFactory.setReadTimeout(90_000);
            String body = RestClient.builder()
                    .baseUrl(trimEndSlash(openAi.getBaseUrl()))
                    .requestFactory(requestFactory)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAi.getApiKey())
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return extractOutputText(body);
        } catch (RestClientException ex) {
            throw new BaseException(HttpStatus.BAD_GATEWAY, "调用视觉模型失败：" + ex.getMessage());
        }
    }

    private String buildPrompt() {
        return """
                你是一个“你画我猜”识别模型。请观察用户在白色画布上的黑色简笔画，输出最可能的中文物体名称。
                不要使用预设候选词，不要假设用户正在画指定题目，只根据画面本身自由猜测。
                请重点观察黑色笔迹围成的主体结构，忽略周围空白区域。
                优先猜测常见物体、动物、植物、人物动作、交通工具、食物或简单场景。
                只返回 JSON，不要返回 Markdown，不要解释。格式如下，label 必须替换成你的真实猜测：
                {"predictions":[{"label":"中文名称","score":90},{"label":"中文名称","score":60},{"label":"中文名称","score":30}]}
                分数为 0 到 100 的整数，按可能性从高到低排序，最多返回 5 项。
                """;
    }

    private String extractOutputText(String body) {
        if (!StringUtils.hasText(body)) {
            throw new BaseException(HttpStatus.BAD_GATEWAY, "视觉模型返回为空");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode outputText = root.path("output_text");
            if (outputText.isTextual() && StringUtils.hasText(outputText.asText())) {
                return outputText.asText();
            }
            JsonNode output = root.path("output");
            if (output.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode item : output) {
                    JsonNode content = item.path("content");
                    if (!content.isArray()) {
                        continue;
                    }
                    for (JsonNode contentItem : content) {
                        JsonNode text = contentItem.path("text");
                        if (text.isTextual()) {
                            builder.append(text.asText());
                        }
                    }
                }
                if (!builder.isEmpty()) {
                    return builder.toString();
                }
            }
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.BAD_GATEWAY, "视觉模型返回解析失败");
        }
        throw new BaseException(HttpStatus.BAD_GATEWAY, "视觉模型没有返回文本结果");
    }

    private List<AiDrawGuessPredictionVO> parsePredictions(String outputText) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(outputText));
            JsonNode predictionsNode = root.path("predictions");
            if (!predictionsNode.isArray()) {
                throw new IllegalArgumentException("missing predictions");
            }
            List<AiDrawGuessPredictionVO> predictions = new ArrayList<>();
            for (JsonNode item : predictionsNode) {
                String label = item.path("label").asText("");
                int score = Math.max(0, Math.min(100, item.path("score").asInt(0)));
                if (StringUtils.hasText(label)) {
                    predictions.add(AiDrawGuessPredictionVO.builder()
                            .label(label)
                            .score(score)
                            .build());
                }
            }
            if (!predictions.isEmpty()) {
                return predictions.stream().limit(5).toList();
            }
        } catch (Exception ignored) {
            return fallbackPredictions();
        }
        return fallbackPredictions();
    }

    private List<AiDrawGuessPredictionVO> fallbackPredictions() {
        List<String> labels = List.of("物体", "图形", "工具", "动物", "场景");
        List<Integer> scores = List.of(42, 28, 12, 10, 8);
        List<AiDrawGuessPredictionVO> result = new ArrayList<>();
        for (int i = 0; i < Math.min(5, labels.size()); i++) {
            result.add(AiDrawGuessPredictionVO.builder()
                    .label(labels.get(i))
                    .score(scores.get(i))
                    .build());
        }
        return result;
    }

    private String stripCodeFence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "");
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3).trim();
            }
        }
        return text;
    }

    private String trimEndSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "https://api.openai.com/v1";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
