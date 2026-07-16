package com.edu.pojo.dto.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseUpdateRequest {
    @Size(max = 200, message = "课程名称不能超过200个字符")
    private String title;
    private String description;
    private String coverUrl;
    private String grade;

    @Min(value = 0, message = "课程公开状态不正确")
    @Max(value = 1, message = "课程公开状态不正确")
    private Integer isPublic;

    @Min(value = 1, message = "课程难度不正确")
    @Max(value = 3, message = "课程难度不正确")
    private Integer difficulty;

    @Min(value = 1, message = "课程类型不正确")
    @Max(value = 3, message = "课程类型不正确")
    private Integer courseType;
}
