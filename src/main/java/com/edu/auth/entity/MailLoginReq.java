package com.edu.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailLoginReq {
    private String email;
    private String captchaKey;
    private String captchaValue;
}
