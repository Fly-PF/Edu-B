package com.edu.pojo.dto.course;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChapterUpdateRequest {
    @Size(max = 200, message = "章节名称不能超过200个字符")
    private String title;
    private Integer duration;
    private Integer sortOrder;
}
