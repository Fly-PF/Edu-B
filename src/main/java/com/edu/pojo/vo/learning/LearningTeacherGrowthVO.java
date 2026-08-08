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
public class LearningTeacherGrowthVO {
    private Long classId;
    private String className;
    private String grade;
    private String modelName;
    private Summary summary;
    private List<LearningTeacherOverviewVO.StudentRisk> risks;
    private LearningCourseProfileVO classProfile;
    private LearningAbilityProfileVO classAbilityProfile;
    private LearningClassTrendVO classTrend;
    private List<LearningStudentTypeProfileVO> studentProfiles;
    private List<LearningStudentAbilityVO> studentAbilities;
    private List<LearningQuestionAccuracyVO> questionAccuracy;
    private List<LearningRiskAlertVO> riskAlerts;
    private List<LearningGrowthCaseVO> cases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer studentCount;
        private Integer courseCount;
        private Integer averageProgress;
        private Integer activeStudents;
        private Integer attentionItems;
        private Integer pendingDecisions;
        private Integer awaitingReview;
        private Integer effectiveCases;
    }
}
