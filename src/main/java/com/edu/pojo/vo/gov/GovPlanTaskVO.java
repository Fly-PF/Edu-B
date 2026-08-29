package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class GovPlanTaskVO {
    private Long id;
    private LocalDate taskDate;
    private String title;
    private String taskType;
    private Integer status;
    private LocalDateTime completedAt;
}
