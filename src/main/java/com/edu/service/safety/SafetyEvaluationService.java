package com.edu.service.safety;

import com.edu.pojo.dto.safety.SafetyEvaluationResultDTO;
import com.edu.pojo.dto.safety.SafetyEvaluationRunRequest;

public interface SafetyEvaluationService {
    SafetyEvaluationResultDTO runEvaluation(SafetyEvaluationRunRequest request);
}
