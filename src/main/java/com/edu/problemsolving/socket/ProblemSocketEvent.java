package com.edu.problemsolving.socket;

import lombok.Data;

@Data
public class ProblemSocketEvent {
    private String eventId;
    private String type;
    private String cardId;
    private String question;
    private String level;
    private String stepId;
    private Long sequence;
}
