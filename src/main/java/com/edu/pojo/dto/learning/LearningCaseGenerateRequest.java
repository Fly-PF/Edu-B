package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LearningCaseGenerateRequest {
    @NotNull(message = "班级不能为空")
    private Long classId;
    @NotNull(message = "课程不能为空")
    private Long courseId;
    @NotNull(message = "学生不能为空")
    private Long studentId;
}
