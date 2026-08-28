package com.edu.service.impl;

import com.edu.common.properties.AIModelProperties;
import com.edu.pojo.vo.ai.AiCompanionContextVO;
import com.edu.pojo.vo.ai.AiCompanionMaterialExcerpt;
import com.edu.pojo.vo.ai.AiCompanionWebSource;
import com.edu.pojo.vo.ai.AiCompanionMessageVO;
import com.edu.pojo.vo.ai.AiCompanionModelResult;
import com.edu.service.AiCompanionModelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCompanionModelServiceImpl implements AiCompanionModelService {
    private final AIModelProperties aiModelProperties;
    private final ObjectMapper objectMapper;

    @Override
    public AiCompanionModelResult generateAnswer(
            AiCompanionContextVO context,
            List<AiCompanionMessageVO> history,
            String question
    ) {
        if (!config().isEnabled() || isBlank(model().getBaseUrl()) || requiresApiKeyWithoutOne()) {
            return modelUnavailable(context, "MODEL_DISABLED");
        }

        try {
            String answer = callSpringAiChatModel(context, history, question);
            return new AiCompanionModelResult(
                    formatEvidenceAnswer(context, answer),
                    "MODEL",
                    model().getModelName(),
                    buildSourceSummary(context),
                    "NORMAL"
            );
        } catch (RuntimeException exception) {
            if (hasCause(exception, java.net.http.HttpTimeoutException.class)) {
                log.warn("智能学伴模型回答超时", exception);
                return modelUnavailable(context, "MODEL_TIMEOUT");
            }
            if (hasCause(exception, java.net.ConnectException.class)) {
                log.warn("智能学伴无法连接云端模型", exception);
                return modelUnavailable(context, "MODEL_UNAVAILABLE");
            }
            log.warn("智能学伴模型调用失败，将使用演示回答", exception);
            return modelUnavailable(context, "MODEL_UNAVAILABLE");
        } catch (Exception exception) {
            log.warn("智能学伴模型调用失败，将使用演示回答", exception);
            return modelUnavailable(context, "MODEL_UNAVAILABLE");
        }
    }

    private String callSpringAiChatModel(
            AiCompanionContextVO context,
            List<AiCompanionMessageVO> history,
            String question
    ) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt(context)));
        history.stream()
                .filter(message -> "USER".equalsIgnoreCase(message.getRole())
                        || "ASSISTANT".equalsIgnoreCase(message.getRole()))
                .skip(Math.max(0, history.size() - 6L))
                .forEach(message -> messages.add("USER".equalsIgnoreCase(message.getRole())
                        ? new UserMessage(message.getContent())
                        : new AssistantMessage(message.getContent())));
        messages.add(new UserMessage(question));

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .apiKey(model().getApiKey())
                .baseUrl(normalizeBaseUrl(model().getBaseUrl()))
                .model(model().getModelName())
                .temperature(0.2)
                .maxTokens(model().getMaxTokens() == null ? 560 : model().getMaxTokens())
                .extraBody(java.util.Map.of("enable_thinking", false))
                .build();
        ChatResponse response = OpenAiChatModel.builder()
                .options(options)
                .observationRegistry(ObservationRegistry.NOOP)
                .build()
                .call(new Prompt(messages));
        if (response == null || response.getResult() == null
                || response.getResult().getOutput() == null
                || isBlank(response.getResult().getOutput().getText())) {
            throw new IllegalStateException("模型没有返回有效回答");
        }
        return response.getResult().getOutput().getText().trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        return value.replaceFirst("/chat/completions/?$", "");
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    private AIModelProperties.Business config() { return aiModelProperties.getCompanion(); }
    private AIModelProperties.Model model() { return config().getChatModel(); }

    private String buildSystemPrompt(AiCompanionContextVO context) {
        return "你是 Edu-F 教育平台的学生端智能学伴。请使用简体中文，先给结论，再用 2 到 4 条说明解释。"
                + "优先依据给定课程上下文回答；资料不足时明确说资料不足，不要编造课程内容或引用。"
                + "对于作业、练习、考试类问题，不要直接给出用户原题的最终答案；应优先提供类似题目、解题思路、参考答案框架和易错点。"
                + "如果用户已经给出自己的思路，要在此基础上继续补全，而不是替他直接写完整答案。"
                + "严格遵守“回答依据和格式”要求。\n"
                + "课程：" + safe(context.getCourseTitle()) + "\n"
                + "课程简介：" + safe(context.getCourseIntro()) + "\n"
                + "当前章节：" + safe(context.getChapterTitle()) + "\n"
                + "当前资源：" + safe(context.getResourceName()) + "\n"
                + "当前章节进度：" + context.getProgress() + "%\n"
                + "课程章节：" + String.join("、", context.getChapterTitles() == null ? List.of() : context.getChapterTitles()) + "\n"
                + "当前章节资源：" + String.join("、", context.getResourceNames() == null ? List.of() : context.getResourceNames())
                + "\n" + buildEvidenceInstructions(context);
    }

    private String buildFallbackAnswer(AiCompanionContextVO context, String question) {
        String chapter = safe(context.getChapterTitle());
        String resource = safe(context.getResourceName());
        if (context.getMatchedMaterials() != null && !context.getMatchedMaterials().isEmpty()) {
            AiCompanionMaterialExcerpt material = context.getMatchedMaterials().getFirst();
            String excerpt = material.content().length() > 280
                    ? material.content().substring(0, 280) + "…"
                    : material.content();
            return "我找到了与这个问题相关的课程资料。\n\n"
                    + "相似题参考：可将当前问题改写为“" + question + "”的变体后再练一次。\n\n"
                    + "参考资料：课程“" + material.resourceName() + "”第" + material.pageNumber() + "页。\n"
                    + excerpt + "\n\n"
                    + "解题建议：先写出已知条件、关键概念和步骤，再根据资料补全答案框架。";
        }
        if (context.getWebSources() != null && !context.getWebSources().isEmpty()) {
            AiCompanionWebSource source = context.getWebSources().getFirst();
            return "本课程教案中暂时没有此问题的直接答案。\n\n"
                    + "类似题思路：先按照课程目标拆成 2 到 3 个小步骤，再对照资料自己完成。\n\n"
                    + "参考资料（联网摘要）：" + source.title() + "\n"
                    + source.snippet() + "\n\n"
                    + "提示：这不是现成答案，只能作为思路参考。";
        }
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
                formatEvidenceAnswer(context, buildFallbackAnswer(context, question)),
                "FALLBACK",
                null,
                buildSourceSummary(context),
                "NORMAL"
        );
    }

    private AiCompanionModelResult modelUnavailable(AiCompanionContextVO context, String mode) {
        String nextStep = context.getNextChapterTitle() == null
                ? "你可以先复习当前章节的核心概念和学习资源。"
                : "你可以先完成当前章节，再继续学习“" + context.getNextChapterTitle() + "”。";
        String content = "MODEL_TIMEOUT".equals(mode)
                ? "模型回答时间较长，暂时没有生成结果。请稍后重试，或先查看当前章节资料。\n\n" + nextStep
                : "云端学习模型暂时不可用。请稍后重试；课程资料和学习进度不会受影响。\n\n" + nextStep;
        return new AiCompanionModelResult(
                content,
                mode,
                null,
                "模型服务暂不可用 / 学习建议",
                "NORMAL"
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
        return model().isApiKeyRequired() && isBlank(model().getApiKey());
    }

    private String buildEvidenceInstructions(AiCompanionContextVO context) {
        if (context.getMatchedMaterials() != null && !context.getMatchedMaterials().isEmpty()) {
            StringBuilder result = new StringBuilder("回答依据和格式：课程资料已命中。系统会自动在你的回答前展示真实的课程资料名称和页码。你只需要围绕问题给出类似题、思路、参考答案框架和易错点，不要直接复述成完整标准答案，也不要自行生成“本题答案来自课程”或“资源名 第X页”等来源文字。\n课程资料如下：\n");
            for (AiCompanionMaterialExcerpt material : context.getMatchedMaterials()) {
                result.append("[资料：")
                        .append(safe(material.resourceName()))
                        .append("，第").append(material.pageNumber()).append("页]\n")
                        .append(material.content()).append("\n");
            }
            return result.toString();
        }
        if (context.getWebSources() != null && !context.getWebSources().isEmpty()) {
            StringBuilder result = new StringBuilder("回答依据和格式：本课程教案没有命中直接答案。回答第一行必须是“本课程教案中暂时没有此问题的直接答案。”；第二行开始请给出类似题、解题思路和参考答案框架，再说明联网资料仅作参考，不属于课程教案。\n联网资料如下：\n");
            for (AiCompanionWebSource source : context.getWebSources()) {
                result.append("[来源：")
                        .append(source.title()).append(" / ").append(source.url()).append("]\n")
                        .append(source.snippet()).append("\n");
            }
            return result.toString();
        }
        return "回答依据和格式：课程资料未覆盖当前问题。回答第一行必须是“课程资料未覆盖该问题，下面提供通用学习参考。”；随后给出类似题、解题思路和参考答案框架。不得直接给出原题完整现成答案，也不要提及联网检索、联网失败或“网上资料显示”。";
    }

    private String formatEvidenceAnswer(AiCompanionContextVO context, String answer) {
        if (context.getMatchedMaterials() != null && !context.getMatchedMaterials().isEmpty()) {
            AiCompanionMaterialExcerpt material = context.getMatchedMaterials().getFirst();
            String prefix = "本题答案来自课程：“" + material.resourceName() + "”第" + material.pageNumber() + "页。";
            return prefix + "\n\n" + removeGeneratedCourseCitation(answer);
        }
        if (context.getWebSources() != null && !context.getWebSources().isEmpty()) {
            String prefix = "本课程教案中暂时没有此问题的答案。\n\n网上资料显示：";
            return answer.startsWith("本课程教案中暂时没有此问题的答案。") ? answer : prefix + "\n" + answer;
        }
        String prefix = "课程资料未覆盖该问题，下面提供通用学习参考。\n\n";
        return answer.startsWith("课程资料未覆盖该问题") ? answer : prefix + answer;
    }

    private String removeGeneratedCourseCitation(String answer) {
        String content = answer == null ? "" : answer.trim();
        if (!content.startsWith("本题答案来自课程")) {
            return content;
        }
        int firstLineEnd = content.indexOf('\n');
        return firstLineEnd < 0 ? "" : content.substring(firstLineEnd + 1).stripLeading();
    }

}
