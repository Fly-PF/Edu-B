package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningWrongBookVO {
    private Long id;
    private String name;
    private Integer questionCount;
    private LocalDateTime updatedAt;
    private List<QuestionItem> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionItem {
        private Long id;
        private Long practiceId;
        private Long questionId;
        private String practiceTitle;
        private String courseName;
        private String questionType;
        private String content;
        private List<String> options;
        private Integer score;
        private Integer awardedScore;
        private String studentAnswer;
        private String referenceAnswer;
        private String explanation;
        private String teacherFeedback;
        private String wrongReason;
        private Integer retrainCount;
        private Boolean mastered;
        private String lastRetrainAnswer;
        private LocalDateTime lastRetrainAt;
    }
}
