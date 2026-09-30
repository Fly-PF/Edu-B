package com.edu.pojo.dto.problem;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProblemSessionCreateRequest {
    @NotNull(message = "请选择讲题")
    private Long problemId;
}
