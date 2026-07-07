package com.edu.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 权限不足全局处理器
 *
 * @author Fly
 * @since 2026-02-26 20:00
 */

@Component
@RequiredArgsConstructor
public class AuthAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        String message = String.format("权限认证失败：%s", accessDeniedException.getMessage());
        Result<Object> result = Result.setResult(HttpStatus.FORBIDDEN, message, null);

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

}
