package com.edu.common.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edu.ai")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProviderProperties {
    private String provider;
    private String baseUrl;
    @ToString.Exclude
    private String apiKey;
    private String model;
    private String gradingModel;
    private String lessonModel;
    private Integer gradingTimeoutMs;
}
