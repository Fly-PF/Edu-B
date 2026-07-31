package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class LearningPlanDecisionRequest {
    @NotBlank(message = "请选择计划处理方式")
    private String decision;
    private String title;
    private String learningGoal;
    private List<String> taskSteps;
    private Integer durationMinutes;
    private String acceptanceCriteria;
}
