package com.edu.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edu.ai.companion")
public class AiCompanionProperties {
    private boolean enabled;
    private String apiUrl;
    private String apiKey;
    private String model;
    private int timeoutSeconds = 30;
    private int maxTokens = 800;
}
