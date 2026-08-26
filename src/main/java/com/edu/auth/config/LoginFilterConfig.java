package com.edu.auth.config;

import com.edu.auth.handler.LoginFailureHandler;
import com.edu.auth.handler.LoginSuccessHandler;
import com.edu.auth.mailLogin.MailAuthenticationFilter;
import com.edu.auth.mailLogin.MailAuthenticationProvider;
import com.edu.auth.usernameLogin.UsernameAuthenticationFilter;
import com.edu.auth.usernameLogin.UsernameAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class LoginFilterConfig {
    private final LoginSuccessHandler loginSuccessHandler;

    private final LoginFailureHandler loginFailureHandler;

    private final UsernameAuthenticationProvider usernameAuthenticationProvider;

    private final MailAuthenticationProvider mailAuthenticationProvider;

    @Bean
    public UsernameAuthenticationFilter usernameAuthenticationFilter() {
        ProviderManager providerManager = new ProviderManager(List.of(usernameAuthenticationProvider));

        return new UsernameAuthenticationFilter(
                request -> {
                    String requestURI = request.getRequestURI();
                    String method = request.getMethod();
                    return method.equals("POST") && requestURI.equals("/api/user/login/username");
                },
                providerManager,
                loginSuccessHandler,
                loginFailureHandler
        );
    }

    @Bean
    public MailAuthenticationFilter mailAuthenticationFilter() {
        ProviderManager providerManager = new ProviderManager(List.of(mailAuthenticationProvider));

        return new MailAuthenticationFilter(
                request -> {
                    String requestURI = request.getRequestURI();
                    String method = request.getMethod();
                    return method.equals("POST") && requestURI.equals("/api/user/login/mail");
                },
                providerManager,
                loginSuccessHandler,
                loginFailureHandler
        );
    }
}
