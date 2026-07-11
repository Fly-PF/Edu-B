package com.edu.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserPasswordRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须为8-32个字符")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须为8-32个字符")
    private String confirmPassword;
}
