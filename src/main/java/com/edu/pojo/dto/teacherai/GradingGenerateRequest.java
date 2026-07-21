package com.edu.pojo.dto.teacherai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GradingGenerateRequest {
    @NotBlank(message = "题目不能为空")
    @Size(max = 3000, message = "题目不能超过3000个字符")
    private String question;

    @NotBlank(message = "题型不能为空")
    @Size(max = 50, message = "题型不能超过50个字符")
    private String questionType;

    @NotBlank(message = "参考答案不能为空")
    @Size(max = 5000, message = "参考答案不能超过5000个字符")
    private String referenceAnswer;

    @Valid
    @NotEmpty(message = "评分标准不能为空")
    @Size(max = 20, message = "评分维度不能超过20项")
    private List<AiRubricItem> rubric;

    @NotBlank(message = "学生答案不能为空")
    @Size(max = 5000, message = "学生答案不能超过5000个字符")
    private String studentAnswer;

    @NotNull(message = "总分不能为空")
    @DecimalMin(value = "0.1", message = "总分必须大于0")
    @Digits(integer = 10, fraction = 1, message = "分值最多保留一位小数")
    private BigDecimal maxScore;
}
