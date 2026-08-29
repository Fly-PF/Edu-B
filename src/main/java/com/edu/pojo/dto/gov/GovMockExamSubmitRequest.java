package com.edu.pojo.dto.gov;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GovMockExamSubmitRequest {
    private boolean autoSubmitted;
    private List<AnswerItem> answers = List.of();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerItem {
        private Long questionId;
        private List<String> selectedAnswers = List.of();
    }
}

