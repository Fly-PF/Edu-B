package com.edu.service.safety;

import com.edu.pojo.dto.safety.RagEvidenceRequest;
import com.edu.pojo.dto.safety.RagEvidenceResponse;

public interface RagEvidenceService {
    RagEvidenceResponse checkEvidence(RagEvidenceRequest request);
}
