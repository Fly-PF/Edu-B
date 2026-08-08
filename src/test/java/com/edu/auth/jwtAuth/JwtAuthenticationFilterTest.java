package com.edu.auth.jwtAuth;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.edu.common.properties.JwtProperties;
import com.edu.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsOnlyHeaderPresenceWithoutPrintingJwt() throws Exception {
        JwtProperties jwtProperties = JwtProperties.builder()
                .jwtName("Authorization")
                .secretKey("0123456789abcdef0123456789abcdef")
                .expireTime(3600000L)
                .build();
        JwtUtil jwtUtil = new JwtUtil();
        Field field = JwtUtil.class.getDeclaredField("jwtProperties");
        field.setAccessible(true);
        field.set(jwtUtil, jwtProperties);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                null,
                null,
                null,
                jwtUtil,
                jwtProperties
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String jwt = "Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature";
        request.addHeader("Authorization", jwt);

        Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            filter.doFilter(request, response, (req, res) -> {
            });
        } finally {
            logger.detachAppender(appender);
        }

        String logs = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertTrue(logs.contains("authorizationHeader present: true"));
        assertFalse(logs.contains(jwt));
        assertFalse(logs.contains("Bearer eyJ"));
    }
}
