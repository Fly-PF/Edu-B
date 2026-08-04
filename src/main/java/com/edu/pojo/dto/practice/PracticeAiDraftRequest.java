package com.edu.pojo.dto.practice;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PracticeAiDraftRequest {
    @NotNull(message = "开放题编号不能为空")
    private Long questionId;

    @NotNull(message = "AI 建议分数不能为空")
    @Min(value = 0, message = "AI 建议分数不能小于 0")
    @Max(value = 100, message = "AI 建议分数不能超过 100")
    private Integer score;

    @NotBlank(message = "AI 建议反馈不能为空")
    @Size(max = 500, message = "AI 建议反馈不能超过 500 字")
    private String feedback;

    @Size(max = 2000, message = "AI 评分理由不能超过 2000 字")
    private String reasoning;

    @DecimalMin(value = "0.0", message = "AI 置信度不能小于 0")
    @DecimalMax(value = "1.0", message = "AI 置信度不能大于 1")
    private BigDecimal confidence;
}
