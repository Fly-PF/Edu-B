package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GovKnowledgeCompareSaveRequest {
    @NotBlank(message = "辨析标题不能为空")
    @Size(max = 200, message = "辨析标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "辨析内容不能为空")
    @Size(max = 50000, message = "辨析内容长度不能超过50000个字符")
    private String contentMd;

    @Min(value = 0, message = "排序不能小于0")
    private Integer sortOrder;

    @Min(value = 0, message = "状态不正确")
    @Max(value = 1, message = "状态不正确")
    private Integer status;
}
