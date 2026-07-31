package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LearningEvidenceSubmitRequest {
    @NotBlank(message = "请填写本次完成情况")
    private String reflection;
    @NotBlank(message = "请填写遇到的困难，未遇到可写无")
    private String difficulty;
    @NotBlank(message = "请回答理解检查问题")
    private String answer;
}
