package com.edu.auth.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.common.Result;
import com.edu.auth.entity.LoginRes;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.repository.SysUserRepository;
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
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
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
    private final SysUserRepository sysUserRepository;

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

        sysUserRepository.updateLastLoginInfo(
                userInfoDTO.getUserId(),
                getClientIp(request),
                LocalDateTime.now()
        );

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

    private String getClientIp(HttpServletRequest request) {
        String ipv4 = null;
        String ipv6 = null;
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);
            String[] ips = parseClientIps(headerValue);
            if (ipv4 == null) {
                ipv4 = ips[0];
            }
            if (ipv6 == null) {
                ipv6 = ips[1];
            }
        }

        String[] remoteIps = parseClientIps(request.getRemoteAddr());
        if (ipv4 == null) {
            ipv4 = remoteIps[0];
        }
        if (ipv6 == null) {
            ipv6 = remoteIps[1];
        }
        return "%s | %s".formatted(ipv4 == null ? "null" : ipv4, ipv6 == null ? "null" : ipv6);
    }

    private String[] parseClientIps(String value) {
        String ipv4 = null;
        String ipv6 = null;
        if (value == null || value.isBlank() || "unknown".equalsIgnoreCase(value.trim())) {
            return new String[]{ipv4, ipv6};
        }

        for (String item : value.split(",")) {
            String ip = normalizeIp(item);
            if (ip == null) {
                continue;
            }
            try {
                InetAddress inetAddress = InetAddress.getByName(ip);
                if (ipv4 == null && inetAddress instanceof Inet4Address) {
                    ipv4 = ip;
                }
                if (ipv6 == null && inetAddress instanceof Inet6Address) {
                    ipv6 = ip;
                }
            } catch (UnknownHostException ignored) {
            }
        }
        return new String[]{ipv4, ipv6};
    }

    private String normalizeIp(String value) {
        if (value == null) {
            return null;
        }
        String ip = value.trim();
        if (ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        if (ip.startsWith("[") && ip.contains("]")) {
            return ip.substring(1, ip.indexOf(']'));
        }
        if (ip.chars().filter(ch -> ch == ':').count() == 1 && ip.contains(".")) {
            return ip.substring(0, ip.indexOf(':'));
        }
        return ip;
    }
}
