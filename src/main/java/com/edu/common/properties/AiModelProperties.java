package com.edu.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edu.ai")
public class AiModelProperties {
    private OpenAi openai = new OpenAi();

    @Data
    public static class OpenAi {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-5.6-luna";
        private Integer timeoutSeconds = 20;
    }
}
