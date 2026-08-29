package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GovMockExamCreateRequest {
    private String subject;

    @NotNull(message = "题目数量不能为空")
    @Min(value = 1, message = "题目数量不能少于 1")
    @Max(value = 100, message = "题目数量不能超过 100")
    private Integer questionCount;

    @Min(value = 1, message = "难度不能低于 1")
    @Max(value = 5, message = "难度不能高于 5")
    private Integer difficulty;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 60, message = "考试时长不能少于 1 分钟")
    @Max(value = 7200, message = "考试时长不能超过 2 小时")
    private Integer durationLimitSeconds;
}

