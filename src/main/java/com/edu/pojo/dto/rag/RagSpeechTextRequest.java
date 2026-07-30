package com.edu.pojo.dto.rag;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagSpeechTextRequest {
    @NotBlank(message = "content 不能为空")
    private String content;
}
