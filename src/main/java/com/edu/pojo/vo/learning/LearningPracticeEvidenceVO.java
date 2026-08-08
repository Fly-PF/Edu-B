package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Reviewed practice results used as persisted evidence for a student's profile. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPracticeEvidenceVO {
    private Integer totalPractices;
    private Integer reviewedPractices;
    private Integer pendingPractices;
    private Integer earnedScore;
    private Integer possibleScore;
    private Integer averageScore;
    private Integer wrongQuestionCount;
    private List<ScoreItem> scores;
    private List<WrongQuestion> wrongQuestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreItem {
        private Long practiceId;
        private String title;
        private String courseName;
        private Integer score;
        private Integer totalScore;
        private Integer percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WrongQuestion {
        private Long practiceId;
        private String practiceTitle;
        private String courseName;
        private Long questionId;
        private String content;
        private Integer score;
        private Integer awardedScore;
        private String referenceAnswer;
        private String explanation;
    }
}
