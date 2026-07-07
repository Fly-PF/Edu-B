package com.edu.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未登录/认证失败全局处理器
 *
 * @author Fly
 * @since 2026-02-26 19:59
 */

@Component
@RequiredArgsConstructor
public class AuthenticationHandler implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        String message = String.format("无凭证，请先登录：%s", authException.getMessage());
        Result<Object> result = Result.setResult(HttpStatus.UNAUTHORIZED, message, null);

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
