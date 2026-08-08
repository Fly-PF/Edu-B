package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LearningWrongBookQuestionRequest {
    @NotNull(message = "练习编号不能为空")
    private Long practiceId;

    @NotNull(message = "题目编号不能为空")
    private Long questionId;
}
