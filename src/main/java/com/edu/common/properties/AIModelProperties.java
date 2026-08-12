package com.edu.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edu.ai-model")
public class AIModelProperties {
    private Business rag = new Business();
    private Business teacherAi = new Business();
    private Business companion = new Business();
    private Business learningAnalysis = new Business();
    private Business safetySemantic = new Business();
    private Business drawGuess = new Business();
    private Face face = new Face();

    @Data
    public static class Face {
        private Tencent tencent = new Tencent();
    }

    @Data
    public static class Tencent {
        private String secretId = "";
        private String secretKey = "";
        private String region = "ap-guangzhou";
        private String endpoint = "iai.tencentcloudapi.com";
        private String version = "2020-03-03";
        private Double compareThreshold = 80D;
        private Integer timeoutSeconds = 20;
    }

    @Data
    public static class Business {
        private boolean enabled = true;
        private WebSearch webSearch;
        private Model chatModel = new Model();
        private Model textModel = new Model();
        private Model multiModel = new Model();
        private Model embeddingModel = new Model();
    }

    @Data
    public static class WebSearch {
        private boolean enabled = false;
        private String url;
        private int timeout;
    }

    @Data
    public static class Model {
        private String supplier;
        private String baseUrl;
        private String apiKey;
        private boolean apiKeyRequired;
        private ModelType modelType;
        private String modelName;
        private Integer maxHistoryMessageCount;
        private Integer timeout;
        private Integer maxTokens;
    }

    public enum ModelType {
        TextModel,
        MultiModel,
        TextEmbedModel
    }
}
