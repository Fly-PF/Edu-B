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
public class LearningTeacherOverviewVO {
    private Long classId;
    private String className;
    private String grade;
    private Summary summary;
    private String modelName;
    private List<StudentRisk> risks;
    private List<Intervention> interventions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer studentCount;
        private Integer courseCount;
        private Integer averageProgress;
        private Integer activeStudents;
        private Integer highRiskItems;
        private Integer mediumRiskItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentRisk {
        private Long studentId;
        private String studentName;
        private Long courseId;
        private String courseName;
        private Integer progress;
        private Integer courseAverage;
        private Integer totalChapters;
        private Integer finishedChapters;
        private Integer studyMinutes;
        private String lastStudyTime;
        private Integer idleDays;
        private String deadline;
        private Integer deadlineDays;
        private Integer riskScore;
        private String riskLevel;
        private Integer estimatedDays;
        private String nextChapter;
        private String recommendation;
        private List<RiskFactor> factors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskFactor {
        private String code;
        private String label;
        private Integer weight;
        private String evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Intervention {
        private Long id;
        private Long classId;
        private Long courseId;
        private String courseName;
        private Long studentId;
        private String studentName;
        private String title;
        private String taskDescription;
        private Integer riskScore;
        private String status;
        private String studentFeedback;
        private String createdAt;
        private String updatedAt;
    }
}
