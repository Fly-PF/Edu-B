package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LearningInterventionCreateRequest {
    @NotNull(message = "班级不能为空")
    private Long classId;

    @NotNull(message = "课程不能为空")
    private Long courseId;

    @NotNull(message = "学生不能为空")
    private Long studentId;

    @NotBlank(message = "行动标题不能为空")
    private String title;

    @NotBlank(message = "行动任务不能为空")
    private String taskDescription;
}
