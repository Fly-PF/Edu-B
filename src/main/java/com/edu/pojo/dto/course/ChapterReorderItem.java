package com.edu.pojo.dto.course;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChapterReorderItem {
    @NotNull(message = "章节ID不能为空")
    private Long id;

    @NotNull(message = "章节排序不能为空")
    private Integer sortOrder;
}
