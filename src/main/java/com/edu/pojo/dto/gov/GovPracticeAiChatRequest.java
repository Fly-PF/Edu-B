package com.edu.pojo.dto.gov;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GovPracticeAiChatRequest {
    private Long skillId;
    private String action;
    private String stem;
    private String subject;
    private String questionType;
    private List<String> options = new ArrayList<>();
    private List<String> correctAnswers = new ArrayList<>();
    private List<String> selectedAnswers = new ArrayList<>();
    private String analysis;
    private String userMessage;
    private String practiceStem;
    private List<String> practiceOptions = new ArrayList<>();
    private String practiceAnswer;
    private List<GovPracticeAiMessageDTO> history = new ArrayList<>();
}
