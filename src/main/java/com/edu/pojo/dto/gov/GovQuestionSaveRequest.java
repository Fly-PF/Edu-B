package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GovQuestionSaveRequest {
    @NotBlank(message = "科目不能为空")
    private String subject;

    @NotBlank(message = "题型不能为空")
    private String questionType;

    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度不能低于1")
    @Max(value = 5, message = "难度不能高于5")
    private Integer difficulty;

    private Integer examYear;

    @NotBlank(message = "题目来源不能为空")
    private String sourceType;

    @NotNull(message = "题目内容不能为空")
    private GovQuestionContentDTO content;

    private Integer status;
}

