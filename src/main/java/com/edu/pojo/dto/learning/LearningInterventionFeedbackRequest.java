package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LearningInterventionFeedbackRequest {
    @NotBlank(message = "请填写本次学习反馈")
    private String feedback;

    private Boolean readyForReview;
}
