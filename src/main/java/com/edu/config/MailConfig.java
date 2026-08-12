package com.edu.config;

import com.edu.common.properties.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(MailProperties mailProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailProperties.getHost());
        if (mailProperties.getPort() != null) {
            mailSender.setPort(mailProperties.getPort());
        }
        mailSender.setUsername(mailProperties.getUsername());
        mailSender.setPassword(mailProperties.getPassword());
        Properties properties = new Properties();
        MailProperties.MailSettings mailSettings = mailProperties.getProperties();
        if (mailSettings != null && mailSettings.getMail() != null
                && mailSettings.getMail().getSmtp() != null) {
            MailProperties.Smtp smtp = mailSettings.getMail().getSmtp();
            if (smtp.getAuth() != null) {
                properties.setProperty("mail.smtp.auth", smtp.getAuth().toString());
            }
            if (smtp.getSsl() != null && smtp.getSsl().getEnable() != null) {
                properties.setProperty("mail.smtp.ssl.enable", smtp.getSsl().getEnable().toString());
            }
        }
        mailSender.setJavaMailProperties(properties);
        return mailSender;
    }
}
