package com.edu.problemsolving.socket;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ProblemSocketEnvelope {
    private String eventId;
    private long sequence;
    private String clientSessionId;
    private String type;
    private String baseRevision;
    private String nextRevision;
    private Map<String, Object> payload;
}
