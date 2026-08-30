package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GovKnowledgeProgressUpdateRequest {
    @NotBlank(message = "学习状态不能为空")
    @Size(max = 20, message = "学习状态长度不能超过20个字符")
    private String status;
}
