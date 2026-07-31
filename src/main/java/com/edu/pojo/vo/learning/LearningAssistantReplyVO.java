package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningAssistantReplyVO {
    private String answer;
    private String nextStep;
    private String recommendedChapter;
    private String source;
    private Boolean llmUsed;
    private List<String> references;
}
