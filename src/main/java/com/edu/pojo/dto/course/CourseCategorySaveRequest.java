package com.edu.pojo.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CourseCategorySaveRequest {
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过100个字符")
    private String name;

    @Min(value = 0, message = "展示排序不能小于0")
    private Integer sortOrder;

    @NotEmpty(message = "请至少选择一个课程标签")
    private List<@NotBlank(message = "课程标签不能为空") String> tags;

    private Boolean matchAll;
}
