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
public class LearningStudentGrowthVO {
    private Summary summary;
    private String modelName;
    private List<LearningStudentOverviewVO.CourseLearning> courses;
    private LearningCourseProfileVO learningProfile;
    private LearningAbilityProfileVO abilityProfile;
    private LearningPracticeEvidenceVO practiceEvidence;
    private List<LearningRiskAlertVO> riskAlerts;
    private List<LearningCourseRecommendationVO> recommendations;
    private LearningGrowthCaseVO priorityCase;
    private List<LearningGrowthCaseVO> cases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer courseCount;
        private Integer averageProgress;
        private Integer studyMinutes;
        private Integer activePlans;
        private Integer completedCycles;
    }
}
