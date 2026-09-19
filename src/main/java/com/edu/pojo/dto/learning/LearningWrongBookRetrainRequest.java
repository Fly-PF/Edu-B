package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearningWrongBookRetrainRequest {
    @NotBlank(message = "重练答案不能为空")
    @Size(max = 5000, message = "重练答案不能超过5000字")
    private String answer;
}
