package com.edu.pojo.dto.problem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProblemCategorySaveRequest {
    @NotNull(message = "请选择科目")
    private Long subjectId;
    @NotBlank(message = "请输入分类名称")
    private String name;
    private String description;
    private Integer sortOrder;
}
