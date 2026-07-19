package com.edu.pojo.vo.ai;

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
public class AiProjectCaseVO {
    private Long id;
    private String projectCode;
    private String projectName;
    private String caseSummary;
    private String gradeBand;
    private String subjectDirection;
    private String projectBackground;
    private List<String> learningGoals;
    private String aiCapability;
    private String practiceType;
    private List<String> taskSteps;
    private List<String> requiredTools;
    private String exampleCode;
    private String submissionRequirements;
    private List<AiRubricItemVO> evaluationRubric;
    private String cover;
    private List<String> tags;
    private Integer challengeLevel;
    private Integer sort;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
