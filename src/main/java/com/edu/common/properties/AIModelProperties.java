package com.edu.common.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edu.ai-model")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModelProperties {
    @Builder.Default
    private Provider openai = new Provider();

    @Builder.Default
    private Provider alibaba = new Provider();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Provider {
        private String apiKey;
        private String baseUrl;

        @Builder.Default
        private Model chatModel = new Model();

        @Builder.Default
        private Model embeddingModel = new Model();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Model {
        private String modelName;
        private String completionsPath;
    }

}
