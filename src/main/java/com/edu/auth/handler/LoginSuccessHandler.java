package com.edu.auth.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.common.Result;
import com.edu.auth.entity.LoginRes;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 12:15
 */
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Authentication authentication
    ) throws IOException {

        UserInfoDTO userInfoDTO = (UserInfoDTO) authentication.getPrincipal();
        if (userInfoDTO == null) {
            Result<?> result = Result.setResult(HttpStatus.BAD_REQUEST, "failed", null);

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return;
        }

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userInfoDTO.getUserId());
        extraClaims.put("username", userInfoDTO.getUsername());
        extraClaims.put("realName", userInfoDTO.getRealName());
        extraClaims.put("roleName", userInfoDTO.getRoleName());
        extraClaims.put("roleCode", userInfoDTO.getRoleCode());

        String jwt = jwtUtil.createJWT(extraClaims);
        String token = "Bearer %s".formatted(jwt);

        LoginRes loginRes = LoginRes.builder()
                .userId(userInfoDTO.getUserId())
                .username(userInfoDTO.getUsername())
                .roleCode(userInfoDTO.getRoleCode())
                .realName(userInfoDTO.getRealName())
                .roleName(userInfoDTO.getRoleName())
                .token(token)
                .build();

        Result<?> result = Result.setResult(HttpStatus.OK, "登录成功！", loginRes);

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
