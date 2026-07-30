package com.edu.pojo.dto.safety;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class SafetyEvaluationRunRequest implements Serializable {
    private String runName;

    @Builder.Default
    private Boolean recordSamples = false;

    @Valid
    @NotEmpty
    @Builder.Default
    private List<SafetyEvaluationSampleRequest> samples = new ArrayList<>();
}
