package com.edu.pojo.dto.teacherai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingGenerateResponse {
    private BigDecimal totalScore;
    private List<GradingDimensionScore> dimensionScores;
    private List<String> strengths;
    private List<String> deductions;
    private List<String> suggestions;
    private String revisedAnswer;
    private BigDecimal confidence;
}
