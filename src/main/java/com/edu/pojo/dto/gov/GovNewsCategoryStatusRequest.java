package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GovNewsCategoryStatusRequest {
    @NotNull(message = "分类状态不能为空")
    @Min(value = 0, message = "分类状态不正确")
    @Max(value = 1, message = "分类状态不正确")
    private Integer status;
}
