package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GovKnowledgeNoteSaveRequest {
    @Size(max = 5000, message = "笔记内容长度不能超过5000个字符")
    private String content;
}
