package com.edu.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 13:48
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsernameLoginReq {
    private String username;
    private String password;
}
