package com.edu.common.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edu.milvus")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilvusProperties {
    private String endpoint;
    private String token;
    private String databaseName;

    @Builder.Default
    private Rag rag = new Rag();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rag {
        private Integer topK;
        private Double scoreThreshold;
        private String collectionName;

        @Builder.Default
        private Vector vector = new Vector();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Vector {
        private Integer dimension;
        private String metricType;
    }
}
