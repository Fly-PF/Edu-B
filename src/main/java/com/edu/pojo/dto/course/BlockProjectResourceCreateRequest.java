package com.edu.pojo.dto.course;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlockProjectResourceCreateRequest {
    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    private String name;
    private Integer sortOrder;
}
