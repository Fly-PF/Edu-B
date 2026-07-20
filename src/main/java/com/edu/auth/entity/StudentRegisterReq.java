package com.edu.auth.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegisterReq {
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$", message = "账号格式错误")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须为8-32个字符")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 32, message = "确认密码长度必须为8-32个字符")
    private String confirmPassword;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 30, message = "姓名最多30个字符")
    private String realName;
}
