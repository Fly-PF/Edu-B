package com.edu.common.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
    private String defaultAvatar;
}
