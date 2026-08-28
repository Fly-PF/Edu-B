package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GovPlanTaskSaveRequest {
    @NotNull(message = "任务日期不能为空")
    private LocalDate taskDate;
    @NotBlank(message = "任务内容不能为空")
    @Size(max = 200, message = "任务内容不能超过200个字符")
    private String title;
    @Size(max = 30, message = "任务类型不能超过30个字符")
    private String taskType;
}
