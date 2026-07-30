package com.edu.pojo.vo.practice;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StudentPracticeDetailVO {
    private Long id;
    private Long courseId;
    private String courseName;
    private String title;
    private String intro;
    private Integer totalScore;
    private String submissionStatus;
    private Integer score;
    private String teacherFeedback;
    private LocalDateTime submitTime;
    private List<Question> questions;

    @Data
    @Builder
    public static class Question {
        private Long id;
        private String type;
        private String content;
        private List<String> options;
        private Integer score;
        private String answer;
        private String referenceAnswer;
        private String explanation;
        private Integer awardedScore;
        private String teacherFeedback;
    }
}
