package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningStudentAbilityVO {
    private Long studentId;
    private String studentName;
    private LearningAbilityProfileVO abilityProfile;
    /** Transparent learning-state bucket for teachers to prioritize intervention. */
    private String learningState;
    private String learningStateLabel;
    private String priorityReason;
    private String recommendedAction;
    private Integer topRiskScore;
    private String topRiskLevel;
    private String topRiskCourse;
}
