package com.edu.service.impl;

import com.edu.pojo.dto.safety.SafetyRecordDTO;
import com.edu.service.safety.SafetyReviewDispatchService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SafetyReviewDispatchServiceImpl implements SafetyReviewDispatchService {
    private final Map<Long, Sinks.One<SafetyRecordDTO>> decisions = new ConcurrentHashMap<>();

    @Override
    public Mono<SafetyRecordDTO> awaitDecision(Long reviewRecordId) {
        if (reviewRecordId == null) {
            return Mono.empty();
        }
        Sinks.One<SafetyRecordDTO> sink = decisions.computeIfAbsent(reviewRecordId, key -> Sinks.one());
        return sink.asMono().doFinally(signal -> decisions.remove(reviewRecordId, sink));
    }

    @Override
    public void publishDecision(SafetyRecordDTO record) {
        if (record == null || record.getId() == null) {
            return;
        }
        decisions.computeIfAbsent(record.getId(), key -> Sinks.one()).tryEmitValue(record);
    }
}
