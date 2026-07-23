package com.edu.service.impl;

import com.edu.common.properties.AiCompanionProperties;
import com.edu.pojo.vo.ai.AiCompanionContextVO;
import com.edu.pojo.vo.ai.AiCompanionMessageVO;
import com.edu.pojo.vo.ai.AiCompanionModelResult;
import com.edu.service.AiCompanionModelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCompanionModelServiceImpl implements AiCompanionModelService {
    private final AiCompanionProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public AiCompanionModelResult generateAnswer(
            AiCompanionContextVO context,
            List<AiCompanionMessageVO> history,
            String question
    ) {
        if (!properties.isEnabled() || requiresApiKeyWithoutOne()) {
            return fallback(context, question);
        }

        try {
            return new AiCompanionModelResult(
                    callCompatibleChatApi(context, history, question),
                    "MODEL",
                    properties.getModel(),
                    buildSourceSummary(context),
                    "NORMAL"
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("智能学伴模型调用被中断，将使用演示回答", exception);
            return fallback(context, question);
        } catch (Exception exception) {
            log.warn("智能学伴模型调用失败，将使用演示回答", exception);
            return fallback(context, question);
        }
    }

    private String callCompatibleChatApi(
            AiCompanionContextVO context,
            List<AiCompanionMessageVO> history,
            String question
    ) throws IOException, InterruptedException {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", buildSystemPrompt(context)));
        history.stream()
                .filter(message -> "USER".equalsIgnoreCase(message.getRole())
                        || "ASSISTANT".equalsIgnoreCase(message.getRole()))
                .skip(Math.max(0, history.size() - 6L))
                .forEach(message -> messages.add(new ChatMessage(
                        "USER".equalsIgnoreCase(message.getRole()) ? "user" : "assistant",
                        message.getContent()
                )));
        messages.add(new ChatMessage("user", question));

        var payload = objectMapper.createObjectNode();
        payload.put("model", properties.getModel());
        payload.put("temperature", 0.2);
        payload.put("max_tokens", properties.getMaxTokens());
        var messageArray = payload.putArray("messages");
        for (ChatMessage message : messages) {
            var messageNode = messageArray.addObject();
            messageNode.put("role", message.role());
            messageNode.put("content", message.content());
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(properties.getApiUrl()))
                .timeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
        if (!isBlank(properties.getApiKey())) {
            requestBuilder.header("Authorization", "Bearer " + properties.getApiKey());
        }

        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                .build()
                .send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("模型接口返回 HTTP " + response.statusCode());
        }

        JsonNode content = objectMapper.readTree(response.body())
                .path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || isBlank(content.asText())) {
            throw new IOException("模型接口没有返回有效回答");
        }
        return content.asText().trim();
    }

    private String buildSystemPrompt(AiCompanionContextVO context) {
        return "你是 Edu-F 教育平台的课程智能学伴。请使用简体中文，先给结论，再用 2 到 4 条说明解释。"
                + "优先依据给定课程上下文回答；资料不足时明确说资料不足，不要编造课程内容或引用。"
                + "不要代替学生完成作业或实验，应给出思路、步骤、检查方法，并鼓励学生自己完成。"
                + "回答末尾用一句话说明参考的章节或资源。\n"
                + "课程：" + safe(context.getCourseTitle()) + "\n"
                + "课程简介：" + safe(context.getCourseIntro()) + "\n"
                + "当前章节：" + safe(context.getChapterTitle()) + "\n"
                + "当前资源：" + safe(context.getResourceName()) + "\n"
                + "当前章节进度：" + context.getProgress() + "%\n"
                + "课程章节：" + String.join("、", context.getChapterTitles() == null ? List.of() : context.getChapterTitles()) + "\n"
                + "当前章节资源：" + String.join("、", context.getResourceNames() == null ? List.of() : context.getResourceNames());
    }

    private String buildFallbackAnswer(AiCompanionContextVO context, String question) {
        String chapter = safe(context.getChapterTitle());
        String resource = safe(context.getResourceName());
        if (question.contains("总结")) {
            return "当前正在学习“" + chapter + "”，对应资源是“" + resource
                    + "”。建议从核心概念、操作步骤和学习结论三个方面整理笔记，再用自己的话复述一遍。";
        }
        if (question.contains("下一步") || question.contains("建议")) {
            return context.getNextChapterTitle() == null
                    ? "当前课程没有检测到未完成的下一章节，可以先复习本节内容并尝试独立完成练习。"
                    : "建议先完成“" + chapter + "”，然后继续学习“" + context.getNextChapterTitle() + "”。";
        }
        return "我已记录你关于“" + question + "”的问题。当前上下文是“" + chapter + " / " + resource
                + "”。请先回看对应资源，再把不理解的概念或步骤具体指出来。";
    }

    private AiCompanionModelResult fallback(AiCompanionContextVO context, String question) {
        return new AiCompanionModelResult(
                buildFallbackAnswer(context, question),
                "FALLBACK",
                null,
                buildSourceSummary(context),
                "NORMAL"
        );
    }

    private String buildSourceSummary(AiCompanionContextVO context) {
        return "课程：" + safe(context.getCourseTitle())
                + " / 章节：" + safe(context.getChapterTitle())
                + " / 资源：" + safe(context.getResourceName());
    }

    private String safe(String value) {
        return isBlank(value) ? "未提供" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean requiresApiKeyWithoutOne() {
        return !isLocalOllama() && isBlank(properties.getApiKey());
    }

    private boolean isLocalOllama() {
        if (isBlank(properties.getApiUrl())) {
            return false;
        }
        try {
            URI uri = URI.create(properties.getApiUrl());
            String host = uri.getHost();
            return "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private record ChatMessage(String role, String content) {
    }
}
