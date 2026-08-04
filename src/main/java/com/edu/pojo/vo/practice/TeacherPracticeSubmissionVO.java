package com.edu.pojo.vo.practice;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TeacherPracticeSubmissionVO {
    private Long submissionId;
    private Long practiceId;
    private String practiceTitle;
    private Long courseId;
    private String courseName;
    private Integer totalScore;
    private Long studentId;
    private String studentName;
    private Integer autoScore;
    private Integer teacherScore;
    private String feedback;
    private String status;
    private LocalDateTime submitTime;
    private LocalDateTime reviewTime;
    private List<Answer> answers;

    @Data
    @Builder
    public static class Answer {
        private Long questionId;
        private String questionType;
        private String questionContent;
        private String studentAnswer;
        private String referenceAnswer;
        private String explanation;
        private Integer score;
        private Integer awardedScore;
        private String teacherFeedback;
    }
}
