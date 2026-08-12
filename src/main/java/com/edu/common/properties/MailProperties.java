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

    private String host;

    private Integer port;

    private String username;

    private String password;

    @Builder.Default
    private String fromName = "Edu-B";

    @Builder.Default
    private MailSettings properties = new MailSettings();

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailSettings {

        @Builder.Default
        private Mail mail = new Mail();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mail {

        @Builder.Default
        private Smtp smtp = new Smtp();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Smtp {

        private Boolean auth;

        @Builder.Default
        private Ssl ssl = new Ssl();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ssl {

        private Boolean enable;
    }
}
