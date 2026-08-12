package com.edu.util;

import com.edu.common.dto.TextEmbeddingDTO;
import com.edu.common.properties.AIModelProperties;
import com.edu.exception.BaseException;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TextEmbeddingUtil {
    private static final int VECTOR_DIMENSION = 2048;

    private final AIModelProperties aiModelProperties;

    public TextEmbeddingDTO embed(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文本不能为空");
        }

        AIModelProperties.Model model = aiModelProperties.getRag().getEmbeddingModel();
        if (model == null || !StringUtils.hasText(model.getApiKey()) || !StringUtils.hasText(model.getBaseUrl())
                || !StringUtils.hasText(model.getModelName())) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 向量配置不完整，请检查 edu.ai-model.rag.embedding-model 相关配置");
        }

        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .options(OpenAiEmbeddingOptions.builder()
                        .apiKey(model.getApiKey())
                        .baseUrl(model.getBaseUrl())
                        .model(model.getModelName())
                        .dimensions(VECTOR_DIMENSION)
                        .build())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();

        return new TextEmbeddingDTO(text, embeddingModel.embed(text));
    }
}
