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
public class LearningQuestionAccuracyVO {
    private Long practiceId;
    private String practiceTitle;
    private Long courseId;
    private String courseName;
    private Integer reviewedSubmissionCount;
    private List<Question> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        private Long questionId;
        private String questionType;
        private String content;
        private Integer score;
        private Integer attemptCount;
        private Integer correctCount;
        private Integer accuracy;
    }
}
