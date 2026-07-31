package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LearningPlanReviewRequest {
    @NotBlank(message = "请选择干预结论")
    private String outcome;
    private String conclusion;
}
