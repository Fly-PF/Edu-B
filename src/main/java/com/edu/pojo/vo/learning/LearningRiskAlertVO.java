package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningRiskAlertVO {
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseName;
    private Integer riskScore;
    private String riskLevel;
    private String title;
    private String evidence;
    private String action;
    private String nextChapter;
}
