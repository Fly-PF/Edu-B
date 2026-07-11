package com.edu.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Integer userType;
    private String grade;
    private String school;
}
