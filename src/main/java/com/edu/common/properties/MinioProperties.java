package com.edu.common.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "edu.minio")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String buckerName;
    private String publicBaseUrl;
    @Builder.Default
    private Avatar avatar = new Avatar();
    @Builder.Default
    private Rag rag = new Rag();
    @Builder.Default
    private Gov gov = new Gov();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Avatar {
        private String avatarFilesBaseUrl;
        private String defaultAvatar;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rag {
        private String ragFilesBaseUrl;
        private DataSize maxRagFileSize;
        private Integer maxRefRagKbCount;
        private Integer maxRefRagDocCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Gov {
        private String govFilesBaseUrl;
        private DataSize maxGovFileSize;
    }
}
