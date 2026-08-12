package com.edu.util;

import com.edu.common.properties.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;

@Component
public class MailUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DIGITS = "0123456789";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;

    public MailUtil(ObjectProvider<JavaMailSender> mailSenderProvider, MailProperties mailProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
    }

    public String generateCaptchaCode() {
        return generateCaptchaCode(getCaptchaLength());
    }

    public String generateCaptchaCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("captcha length must be greater than 0");
        }

        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        return code.toString();
    }

    public String sendCaptchaMail(String to) {
        String code = generateCaptchaCode();
        sendCaptchaMail(to, code, Duration.ofMinutes(getCaptchaExpireMinutes()));
        return code;
    }

    public void sendCaptchaMail(String to, String code) {
        sendCaptchaMail(to, code, Duration.ofMinutes(getCaptchaExpireMinutes()));
    }

    public void sendCaptchaMail(String to, String code, Duration expireTime) {
        requireText(code, "captcha code");
        if (expireTime == null || expireTime.isNegative() || expireTime.isZero()) {
            throw new IllegalArgumentException("expire time must be greater than 0");
        }

        long minutes = expireTime.toMinutes();
        String html = """
                <div style=\"font-family:Arial,sans-serif;line-height:1.7;color:#1f2937;\">
                    <p>Your verification code is:</p>
                    <p style=\"font-size:28px;font-weight:700;letter-spacing:4px;margin:12px 0;\">%s</p>
                    <p>This code is valid for %d minutes. Please do not share it with anyone.</p>
                </div>
                """.formatted(code, Math.max(minutes, 1));

        sendHtmlMail(to, "Edu-B Verification Code", html);
    }

    public void sendTextMail(String to, String subject, String content) {
        sendMail(to, subject, content, false);
    }

    public void sendHtmlMail(String to, String subject, String htmlContent) {
        sendMail(to, subject, htmlContent, true);
    }

    private void sendMail(String to, String subject, String content, boolean html) {
        requireText(to, "recipient");
        requireText(subject, "subject");
        requireText(content, "content");

        JavaMailSender mailSender = getMailSender();
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            setFrom(helper);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, html);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("failed to prepare mail", e);
        }
    }

    private JavaMailSender getMailSender() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is not configured. Please configure edu.mail settings first.");
        }
        return mailSender;
    }

    private void setFrom(MimeMessageHelper helper) throws MessagingException, UnsupportedEncodingException {
        String fromAddress = mailProperties.getUsername();
        if (!StringUtils.hasText(fromAddress)) {
            return;
        }
        String fromName = mailProperties.getFromName();
        if (StringUtils.hasText(fromName)) {
            helper.setFrom(fromAddress, fromName);
            return;
        }
        helper.setFrom(fromAddress);
    }

    private int getCaptchaLength() {
        MailProperties.Captcha captcha = mailProperties.getCaptcha();
        if (captcha == null || captcha.getLength() == null) {
            return 6;
        }
        return captcha.getLength();
    }

    private long getCaptchaExpireMinutes() {
        MailProperties.Captcha captcha = mailProperties.getCaptcha();
        if (captcha == null || captcha.getExpireMinutes() == null) {
            return 5L;
        }
        return captcha.getExpireMinutes();
    }

    private void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
