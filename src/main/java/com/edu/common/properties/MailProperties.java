package com.edu.common.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edu.mail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailProperties {

    @Builder.Default
    private String fromName = "Edu-B";

    private String fromAddress;

    @Builder.Default
    private Captcha captcha = new Captcha();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Captcha {

        @Builder.Default
        private Integer length = 6;

        @Builder.Default
        private Long expireMinutes = 5L;
    }
}
