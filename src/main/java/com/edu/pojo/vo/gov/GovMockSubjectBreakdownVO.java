package com.edu.pojo.vo.gov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovMockSubjectBreakdownVO {
    private String subject;
    private Integer totalCount;
    private Integer correctCount;
    private BigDecimal accuracyRate;
}

