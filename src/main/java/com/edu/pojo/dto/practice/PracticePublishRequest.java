package com.edu.pojo.dto.practice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PracticePublishRequest {
    @NotNull(message = "请选择课程")
    private Long courseId;
    @NotBlank(message = "请填写练习标题")
    @Size(max = 120, message = "练习标题不能超过 120 字")
    private String title;
    @Size(max = 500, message = "练习说明不能超过 500 字")
    private String intro;
    @NotNull(message = "请填写练习总分")
    @Min(value = 1, message = "练习总分至少为 1 分")
    @Max(value = 100, message = "练习总分不能超过 100 分")
    private Integer totalScore;
    @Valid
    @NotEmpty(message = "请至少添加一道题目")
    @Size(max = 30, message = "单份练习最多 30 道题")
    private List<Question> questions;

    @Data
    public static class Question {
        @NotBlank(message = "请选择题目类型")
        private String type;
        @NotBlank(message = "请填写题干")
        @Size(max = 3000, message = "题干不能超过 3000 字")
        private String content;
        private List<@Size(max = 300, message = "选项不能超过 300 字") String> options;
        @NotBlank(message = "请填写参考答案")
        @Size(max = 3000, message = "参考答案不能超过 3000 字")
        private String referenceAnswer;
        @Size(max = 3000, message = "解析不能超过 3000 字")
        private String explanation;
        @NotNull(message = "请填写题目分值")
        @Min(value = 1, message = "题目分值至少为 1 分")
        @Max(value = 100, message = "题目分值不能超过 100 分")
        private Integer score;
    }
}
