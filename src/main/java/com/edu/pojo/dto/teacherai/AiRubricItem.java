package com.edu.pojo.dto.teacherai;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRubricItem {
    @NotBlank(message = "评分维度不能为空")
    @Size(max = 100, message = "评分维度不能超过100个字符")
    private String criterion;

    @NotBlank(message = "评分说明不能为空")
    @Size(max = 500, message = "评分说明不能超过500个字符")
    private String description;

    @NotNull(message = "评分维度分值不能为空")
    @DecimalMin(value = "0.1", message = "评分维度分值必须大于0")
    @Digits(integer = 10, fraction = 1, message = "分值最多保留一位小数")
    private BigDecimal maxScore;
}
