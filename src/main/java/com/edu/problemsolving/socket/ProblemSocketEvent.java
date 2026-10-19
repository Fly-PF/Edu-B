package com.edu.problemsolving.socket;

import lombok.Data;

import java.util.List;

@Data
public class ProblemSocketEvent {
    private String eventId;
    private String type;
    private String cardId;
    private String question;
    private String level;
    private Long skillId;
    private String action;
    private String practiceStem;
    private List<String> practiceOptions;
    private String practiceAnswer;
    private String stepId;
    private Long sequence;
}
