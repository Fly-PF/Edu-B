package com.edu.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 13:49
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRes {
    private Long userId;
    private String username;
    private String email;
    private String token;
}
