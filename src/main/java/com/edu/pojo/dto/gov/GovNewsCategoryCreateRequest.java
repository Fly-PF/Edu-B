package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GovNewsCategoryCreateRequest {
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String name;
    @NotNull(message = "排序值不能为空")
    @Min(value = 0, message = "排序值不正确")
    @Max(value = 100000, message = "排序值不正确")
    private Integer sortOrder;
}
