package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class GovGoalVO {
    private Long id;
    private String examType;
    private String examName;
    private LocalDate examDate;
    private String note;
}
