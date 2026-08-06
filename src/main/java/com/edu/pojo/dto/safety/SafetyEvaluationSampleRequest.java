package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class SafetyEvaluationSampleRequest implements Serializable {
    private String sampleId;

    @Valid
    @NotNull
    private SafetyGatewayRequest request;

    @NotNull
    private SafetyDecision expectedDecision;

    @Builder.Default
    private List<SafetyRiskType> expectedRiskTypes = new ArrayList<>();

    private SafetyEvidenceLevel expectedEvidenceLevel;
    private String note;
}
