package com.edu.pojo.dto.problem;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProblemStatusRequest {
    @NotNull(message = "请选择状态")
    private Integer status;
}
