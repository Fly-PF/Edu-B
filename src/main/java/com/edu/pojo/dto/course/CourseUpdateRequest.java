package com.edu.pojo.dto.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CourseUpdateRequest {
    @Size(max = 200, message = "课程名称不能超过200个字符")
    private String title;
    private String description;
    private List<String> tags;
    private String coverUrl;
    @Size(max = 100, message = "课程系列不能超过100个字符")
    private String seriesName;
    @Min(value = 0, message = "系列内排序不能小于0")
    private Integer seriesOrder;
    private String grade;

    @Min(value = 1, message = "课程难度不正确")
    @Max(value = 3, message = "课程难度不正确")
    private Integer difficulty;

    @Min(value = 1, message = "课程类型不正确")
    @Max(value = 3, message = "课程类型不正确")
    private Integer courseType;
}
