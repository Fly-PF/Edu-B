package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SemanticReviewResponse implements Serializable {
    private SafetyDecision decision;
    private SafetyRiskLevel riskLevel;

    @Builder.Default
    private List<SafetyRiskType> riskTypes = new ArrayList<>();

    private String reason;
    private String suggestion;
    private Double confidence;
    private String source;
}
