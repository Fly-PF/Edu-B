package com.edu.pojo.dto.practice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PracticeAnswerRequest {
    @NotNull(message = "题目不能为空")
    private Long questionId;
    @NotBlank(message = "请填写答案")
    private String answer;
}
