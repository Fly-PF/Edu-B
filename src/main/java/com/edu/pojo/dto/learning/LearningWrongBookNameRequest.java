package com.edu.pojo.dto.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearningWrongBookNameRequest {
    @NotBlank(message = "错题本名称不能为空")
    @Size(max = 40, message = "错题本名称不能超过40个字")
    private String name;
}
