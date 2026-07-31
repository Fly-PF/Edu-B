package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LearningAssistantRequest {
    /** Optional focus course. The server still derives the rest of the context from real records. */
    private Long courseId;

    @NotBlank(message = "请输入课程问题")
    private String question;
}
