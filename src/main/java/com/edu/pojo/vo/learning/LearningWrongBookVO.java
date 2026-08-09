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
        private Long practiceId;
        private Long questionId;
        private String practiceTitle;
        private String courseName;
        private String content;
        private Integer score;
        private Integer awardedScore;
        private String referenceAnswer;
        private String explanation;
    }
}
