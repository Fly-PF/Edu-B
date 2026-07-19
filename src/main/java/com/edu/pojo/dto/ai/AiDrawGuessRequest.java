package com.edu.pojo.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AiDrawGuessRequest {
    @NotBlank(message = "画布图片不能为空")
    private String imageDataUrl;

    private String targetWord;

    private List<String> candidateLabels;
}
