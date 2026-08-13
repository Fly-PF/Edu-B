package com.edu.service.safety;

import com.edu.pojo.dto.safety.SafetyRecordDTO;
import reactor.core.publisher.Mono;

public interface SafetyReviewDispatchService {
    Mono<SafetyRecordDTO> awaitDecision(Long reviewRecordId);

    void publishDecision(SafetyRecordDTO record);
}
