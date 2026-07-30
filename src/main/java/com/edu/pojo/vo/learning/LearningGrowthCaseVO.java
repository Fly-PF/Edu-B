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
public class LearningGrowthCaseVO {
    private Long caseId;
    private Long classId;
    private Long courseId;
    private Long chapterId;
    private String courseName;
    private String chapterName;
    private Long studentId;
    private String studentName;
    private Integer riskScore;
    private String riskLevel;
    private Integer progress;
    private Integer courseAverage;
    private Integer studyMinutes;
    private Integer idleDays;
    private List<LearningTeacherOverviewVO.RiskFactor> factors;
    private String diagnosis;
    private String diagnosisSource;
    private String modelName;
    private String status;
    private String createdAt;
    private String updatedAt;

    private Long planId;
    private String planStatus;
    private String teacherDecision;
    private String title;
    private String learningGoal;
    private List<String> taskSteps;
    private Integer durationMinutes;
    private String acceptanceCriteria;
    private String checkQuestion;
    private List<String> expectedSignals;

    private Long evidenceId;
    private String reflection;
    private String difficulty;
    private String answer;
    private String aiAssessment;
    private Integer confidence;
    private String evidenceResult;
    private String assessmentSource;
    private String teacherConclusion;
    private String submittedAt;
    private String reviewedAt;

    private Integer progressChange;
    private Integer studyMinutesChange;
    private String improvementSummary;
}
