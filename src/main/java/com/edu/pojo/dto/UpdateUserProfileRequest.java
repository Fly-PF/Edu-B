package com.edu.pojo.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public class UpdateUserProfileRequest {
    @JsonIgnore
    private final Set<String> presentFields = new HashSet<>();

    @JsonIgnore
    private final Set<String> unknownFields = new HashSet<>();

    @Size(max = 30, message = "姓名最多30个字符")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @Email(message = "邮箱格式错误")
    @Size(max = 100, message = "邮箱最多100个字符")
    private String email;

    @Size(max = 20, message = "学段最多20个字符")
    private String grade;

    @Size(max = 100, message = "学校名称最多100个字符")
    private String school;

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
}
