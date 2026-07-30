package com.edu.common.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edu.safety.semantic")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetySemanticProperties {
    private String provider;
    private String apiKey;
    private String endpoint;
    private String model;
    private Integer timeoutSeconds;
    private Boolean enableThinking;
}
