package com.edu.service.safety;

import com.edu.pojo.dto.safety.SemanticReviewRequest;
import com.edu.pojo.dto.safety.SemanticReviewResponse;

public interface SemanticReviewService {
    SemanticReviewResponse review(SemanticReviewRequest request);
}
