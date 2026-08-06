package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
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
public class RagEvidenceResponse implements Serializable {
    private SafetyEvidenceLevel evidenceLevel;
    private Double score;
    private String reason;
    private String source;

    @Builder.Default
    private List<RagEvidenceReferenceDTO> references = new ArrayList<>();
}
