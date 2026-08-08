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
public class LearningStudentOverviewVO {
    private Summary summary;
    private String modelName;
    private List<CourseLearning> courses;
    private List<LearningTeacherOverviewVO.Intervention> interventions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer courseCount;
        private Integer averageProgress;
        private Integer studyMinutes;
        private Integer attentionCourses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseLearning {
        private Long classId;
        private String className;
        private Long courseId;
        private String courseName;
        private Integer courseType;
        private String courseTypeName;
        private String courseCategory;
        private Integer difficulty;
        private Integer progress;
        private Integer totalChapters;
        private Integer finishedChapters;
        private Integer studyMinutes;
        private String lastStudyTime;
        private Integer idleDays;
        private Integer riskScore;
        private String riskLevel;
        private String deadline;
        private Integer estimatedDays;
        private String nextChapter;
        private String recommendation;
        private List<LearningTeacherOverviewVO.RiskFactor> factors;
    }
}
