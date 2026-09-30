package com.edu.pojo.dto.problem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProblemAuthoringRequest {
    @NotNull(message = "请选择题型分类")
    private Long categoryId;
    @NotBlank(message = "请输入题目名称")
    private String title;
    @NotBlank(message = "请输入题目简介")
    private String intro;
    @NotBlank(message = "请选择难度")
    private String difficulty;
    private List<String> tags;
    @NotBlank(message = "请输入标准答案")
    private String standardAnswer;
    @NotBlank(message = "请输入答案解析")
    private String answerAnalysis;
    @NotNull(message = "请填写题目定义")
    private Map<String, Object> definition;
}
