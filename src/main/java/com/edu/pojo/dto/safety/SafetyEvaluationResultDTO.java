package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyEvaluationResultDTO implements Serializable {
    private String runName;
    private Integer totalSamples;
    private Integer decisionCorrectCount;
    private Double decisionAccuracy;
    private Integer riskExactMatchCount;
    private Double riskExactMatchRate;
    private Integer evidenceMatchCount;
    private Integer evidenceEvaluatedCount;
    private Double evidenceMatchRate;
    private Double macroRecall;
    private Double macroFalsePositiveRate;
    private Double macroFalseNegativeRate;

    @Builder.Default
    private List<RiskMetricItem> riskMetrics = new ArrayList<>();

    @Builder.Default
    private List<SampleResultItem> sampleResults = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskMetricItem implements Serializable {
        private SafetyRiskType riskType;
        private Integer truePositive;
        private Integer falsePositive;
        private Integer trueNegative;
        private Integer falseNegative;
        private Double recall;
        private Double falsePositiveRate;
        private Double falseNegativeRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SampleResultItem implements Serializable {
        private String sampleId;
        private SafetyDecision expectedDecision;
        private SafetyDecision actualDecision;
        private Boolean decisionMatched;

        @Builder.Default
        private List<SafetyRiskType> expectedRiskTypes = new ArrayList<>();

        @Builder.Default
        private List<SafetyRiskType> actualRiskTypes = new ArrayList<>();

        private Boolean riskTypesMatched;
        private SafetyEvidenceLevel expectedEvidenceLevel;
        private SafetyEvidenceLevel actualEvidenceLevel;
        private Boolean evidenceLevelMatched;
        private String reason;
        private String suggestion;
    }
}
