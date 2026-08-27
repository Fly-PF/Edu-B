package com.edu.controller;

import com.edu.service.GovNewsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(GovNewsAdminControllerSecurityTest.Config.class)
class GovNewsAdminControllerSecurityTest {
    @Autowired
    private GovNewsAdminController controller;
    @Autowired
    private GovNewsService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonAdminCannotAccessAdminController() {
        authenticate("STUDENT");

        assertThatThrownBy(controller::listCategories)
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void adminAuthorityCanAccessAdminController() {
        authenticate("ADMIN");
        when(service.listAdminCategories()).thenReturn(List.of());

        assertThatCode(controller::listCategories).doesNotThrowAnyException();
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "tester",
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                )
        );
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        GovNewsService govNewsService() {
            return mock(GovNewsService.class);
        }

        @Bean
        GovNewsAdminController govNewsAdminController(GovNewsService service) {
            return new GovNewsAdminController(service);
        }
    }
}
