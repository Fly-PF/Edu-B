package com.edu.pojo.dto.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResourceCreateRequest {
    @NotBlank(message = "资源名称不能为空")
    @Size(max = 200, message = "资源名称不能超过200个字符")
    private String name;

    @NotNull(message = "资源类型不能为空")
    @Min(value = 1, message = "资源类型不正确")
    @Max(value = 4, message = "资源类型不正确")
    private Integer type;

    @NotBlank(message = "资源地址不能为空")
    private String url;
    private Long fileSize;
    private Integer duration;
    private Integer sortOrder;
}
