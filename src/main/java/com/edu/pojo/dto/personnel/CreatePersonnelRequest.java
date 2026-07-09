package com.edu.pojo.dto.personnel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonnelRequest {
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$", message = "账号格式错误")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须为8-32个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 30, message = "姓名最多30个字符")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @Email(message = "邮箱格式错误")
    @Size(max = 100, message = "邮箱最多100个字符")
    private String email;

    @Size(max = 255, message = "头像地址最多255个字符")
    private String avatar;

    @Size(max = 20, message = "学段最多20个字符")
    private String grade;

    @Size(max = 100, message = "学校名称最多100个字符")
    private String school;

    @Min(value = 0, message = "状态值错误")
    @Max(value = 1, message = "状态值错误")
    private Integer status;

    private Map<String, Object> extJson;
}
