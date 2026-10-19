package com.edu.pojo.dto.demo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PsDemoProblemAiAskRequest {
    @NotBlank(message = "题目编号不能为空")
    private String problemCode;
    @Min(value = 1, message = "当前步骤不正确")
    @Max(value = 4, message = "当前步骤不正确")
    private Integer currentStep;
    @NotBlank(message = "请输入想问的问题")
    private String question;
    @Size(max = 12, message = "对话历史过长")
    private List<PsDemoProblemAiMessageDTO> history;
}
