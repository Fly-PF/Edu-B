package com.edu.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edu.ai.face")
public class FaceRecognitionProperties {
    private Tencent tencent = new Tencent();

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
}
