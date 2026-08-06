package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SafetyGatewayResponse implements Serializable {
    private boolean allowed;
    private SafetyRiskLevel riskLevel;

    @Builder.Default
    private List<SafetyRiskType> riskTypes = new ArrayList<>();

    private SafetyDecision decision;
    private String reason;
    private String suggestion;
    private String processedText;
    private SafetyEvidenceLevel evidenceLevel;
    private Double evidenceScore;
    private Long recordId;
    private Boolean manualReviewRequired;
    private Boolean teacherConfirmationRequired;

    @Builder.Default
    private Map<String, Object> debugInfo = new LinkedHashMap<>();
}
