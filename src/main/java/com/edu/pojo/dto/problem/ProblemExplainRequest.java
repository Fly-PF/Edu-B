package com.edu.pojo.dto.problem;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProblemExplainRequest {
    @NotBlank(message = "请选择当前讲题步骤")
    private String cardId;
    @NotBlank(message = "请输入想问的问题")
    private String question;
    private String level;
}
