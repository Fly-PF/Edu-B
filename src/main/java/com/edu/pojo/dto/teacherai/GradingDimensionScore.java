package com.edu.pojo.dto.teacherai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingDimensionScore {
    private String criterion;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String reason;
}
