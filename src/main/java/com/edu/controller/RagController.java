package com.edu.controller;

import com.edu.common.properties.AIModelProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.micrometer.observation.ObservationRegistry;
import reactor.core.Disposable;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
@Tag(name = "RAG")
public class RagController {

    private final AIModelProperties aiModelProperties;

    @Operation(summary = "测试AI聊天接口")
    @PostMapping(value = "/chat/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatTest(@Valid @RequestBody ChatTestReq request) {
        SseEmitter emitter = new SseEmitter(0L);

        AIModelProperties.Provider provider = aiModelProperties.getOpenai();
        if (provider == null || !StringUtils.hasText(provider.getApiKey()) || !StringUtils.hasText(provider.getBaseUrl())
                || provider.getChatModel() == null || !StringUtils.hasText(provider.getChatModel().getModelName())) {
            send(emitter, "AI 配置不完整，请检查 edu.ai-model.openai 相关配置");
            emitter.complete();
            return emitter;
        }

        OpenAiChatModel openAiChatModel = buildOpenAiChatModel(provider);
        Prompt prompt = new Prompt(new UserMessage(request.getMessage()));
        Disposable disposable = openAiChatModel.stream(prompt)
                .map(ChatResponse::getResult)
                .filter(generation -> generation != null)
                .map(Generation::getOutput)
                .map(message -> message.getText())
                .filter(StringUtils::hasLength)
                .subscribe(
                        content -> send(emitter, content),
                        error -> {
                            send(emitter, "AI 调用失败: " + error.getMessage());
                            emitter.completeWithError(error);
                        },
                        emitter::complete
                );

        emitter.onCompletion(disposable::dispose);
        emitter.onTimeout(disposable::dispose);

        return emitter;
    }

    private OpenAiChatModel buildOpenAiChatModel(AIModelProperties.Provider provider) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(provider.getChatModel().getModelName())
                .build();

        return OpenAiChatModel.builder()
                .options(options)
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    private void send(SseEmitter emitter, String content) {
        try {
            emitter.send(SseEmitter.event().data(content));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    @Data
    public static class ChatTestReq {
        @NotBlank(message = "message 不能为空")
        private String message;
    }
}
