package com.edu.auth.config;

import com.edu.auth.handler.AuthAccessDeniedHandler;
import com.edu.auth.handler.AuthenticationHandler;
import com.edu.auth.jwtAuth.JwtAuthenticationFilter;
import com.edu.auth.mailLogin.MailAuthenticationFilter;
import com.edu.auth.usernameLogin.UsernameAuthenticationFilter;
import jakarta.servlet.DispatcherType;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.http.HttpMethod.GET;

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        corsConfig.addAllowedOriginPattern("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    private void commonHttpSetting(HttpSecurity http) {
        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                .requestMatchers(
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/api/user/register/student",
                        "/api/user/avatar/image",
                        "/api/rag/kb/cover",
                        "/api/rag/chat/image",
                        "/api/rag/files/preview",
                        "/api/course-files/**"
                ).permitAll()
                .requestMatchers(GET, "/api/course-categories", "/api/course-categories/tags").permitAll()
                .requestMatchers(GET, "/api/ai-exhibit/overview", "/api/ai-exhibit/cases").permitAll()
                .requestMatchers(GET,
                        "/api/rag/kb/public",
                        "/api/rag/kb/public/page",
                        "/api/rag/kb/public/documents").permitAll()
                .requestMatchers(GET, "/api/courses", "/api/courses/*", "/api/courses/*/chapters").permitAll()
        );

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationHandler)
                .accessDeniedHandler(accessDeniedHandler)
        );

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
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
