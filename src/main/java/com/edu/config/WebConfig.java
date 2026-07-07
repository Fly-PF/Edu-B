package com.edu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 12:47
 */
@Configuration
public class WebConfig {

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }


}
