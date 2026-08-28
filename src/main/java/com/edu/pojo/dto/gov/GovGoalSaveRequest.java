package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GovGoalSaveRequest {
    @Size(max = 30, message = "考试类型不能超过30个字符")
    private String examType;
    @NotBlank(message = "目标考试名称不能为空")
    @Size(max = 100, message = "目标考试名称不能超过100个字符")
    private String examName;
    @NotNull(message = "考试日期不能为空")
    private LocalDate examDate;
    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;
}
