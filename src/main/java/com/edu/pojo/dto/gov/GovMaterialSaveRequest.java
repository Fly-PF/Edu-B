package com.edu.pojo.dto.gov;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GovMaterialSaveRequest {
    @NotNull(message = "所属分类不能为空")
    private Long categoryId;

    @NotBlank(message = "资料标题不能为空")
    @Size(max = 200, message = "资料标题不能超过200个字符")
    private String title;

    @Size(max = 1000, message = "资料说明不能超过1000个字符")
    private String description;

    @NotNull(message = "资料类型不能为空")
    private Integer materialType;

    @Valid
    private List<GovMaterialLinkDTO> links;

    @Min(value = 0, message = "展示排序不能小于0")
    private Integer sortOrder;

    private Integer status;
}
