package com.edu.auth.usernameLogin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.auth.entity.UsernameLoginReq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;


import java.io.InputStream;

/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 12:12
 */
public class UsernameAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public UsernameAuthenticationFilter(
            RequestMatcher pathRequestMatcher,
            AuthenticationManager authenticationManager,
            AuthenticationSuccessHandler authenticationSuccessHandler,
            AuthenticationFailureHandler authenticationFailureHandler
    ) {
        super(pathRequestMatcher);
        setAuthenticationManager(authenticationManager);
        setAuthenticationSuccessHandler(authenticationSuccessHandler);
        setAuthenticationFailureHandler(authenticationFailureHandler);
    }

    @Override
    public @Nullable Authentication attemptAuthentication(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response
    ) throws AuthenticationException {
        UsernameLoginReq usernameLoginReq;

        try (InputStream inputStream = request.getInputStream()) {
            ObjectMapper objectMapper = new ObjectMapper();
            usernameLoginReq =  objectMapper.readValue(inputStream, UsernameLoginReq.class);
        } catch (Exception e) {
            throw new AuthenticationServiceException("登录请求格式错误：" + e.getMessage());
        }

        UsernameAuthentication authentication = new UsernameAuthentication();
        authentication.setUsernameLoginReq(usernameLoginReq);
        authentication.setAuthenticated(false);

        return getAuthenticationManager().authenticate(authentication);
    }
}
