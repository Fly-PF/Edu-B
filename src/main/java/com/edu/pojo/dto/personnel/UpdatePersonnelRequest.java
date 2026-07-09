package com.edu.pojo.dto.personnel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdatePersonnelRequest {
    @JsonIgnore
    private final Set<String> presentFields = new HashSet<>();

    @JsonIgnore
    private final Set<String> unknownFields = new HashSet<>();

    @Size(min = 8, max = 32, message = "密码长度必须为8-32个字符")
    private String password;

    @Size(min = 1, max = 30, message = "姓名长度必须为1-30个字符")
    private String realName;

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

    public Set<String> getPresentFields() {
        return presentFields;
    }

    public Set<String> getUnknownFields() {
        return unknownFields;
    }

    @JsonAnySetter
    public void addUnknownField(String name, Object value) {
        unknownFields.add(name);
    }

    public boolean hasField(String fieldName) {
        return presentFields.contains(fieldName);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        presentFields.add("password");
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        presentFields.add("realName");
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        presentFields.add("phone");
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        presentFields.add("email");
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        presentFields.add("avatar");
        this.avatar = avatar;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        presentFields.add("grade");
        this.grade = grade;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        presentFields.add("school");
        this.school = school;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        presentFields.add("status");
        this.status = status;
    }

    public Map<String, Object> getExtJson() {
        return extJson;
    }

    public void setExtJson(Map<String, Object> extJson) {
        presentFields.add("extJson");
        this.extJson = extJson;
    }
}
