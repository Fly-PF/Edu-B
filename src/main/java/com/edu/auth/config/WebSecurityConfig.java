package com.edu.auth.config;

import com.edu.auth.jwtAuth.JwtAuthenticationFilter;
import com.edu.auth.handler.AuthAccessDeniedHandler;
import com.edu.auth.handler.AuthenticationHandler;
import com.edu.auth.mailLogin.MailAuthenticationFilter;
import com.edu.auth.usernameLogin.UsernameAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 12:20
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final AuthenticationHandler authenticationHandler;
    private final AuthAccessDeniedHandler accessDeniedHandler;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UsernameAuthenticationFilter usernameAuthenticationFilter;
    private final MailAuthenticationFilter mailAuthenticationFilter;

    private void commonHttpSetting(HttpSecurity http) {
        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        "/doc.html",          // Knife4j主页面
                        "/v3/api-docs/**",    // OpenAPI3接口文档数据
                        "/webjars/**",        // Knife4j静态资源
                        "/swagger-resources/**"  // 文档资源信息

                ).permitAll()
        );

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationHandler) // 未登录处理
                .accessDeniedHandler(accessDeniedHandler) // 权限不足处理

        );
    }

    @Bean
    public SecurityFilterChain loginFilterChain(HttpSecurity http) {
        commonHttpSetting(http);

        http.securityMatcher("/user/login/**")
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(usernameAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(mailAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public SecurityFilterChain commonFilterChain(HttpSecurity http) {
        commonHttpSetting(http);

        http.authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
