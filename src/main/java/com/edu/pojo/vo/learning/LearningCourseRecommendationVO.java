package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningCourseRecommendationVO {
    private Long courseId;
    private String courseName;
    private String courseCategory;
    private String courseTypeName;
    private Integer courseType;
    private Integer difficulty;
    private String intro;
    private Integer score;
    private String reason;
    private String source;
    private String modelName;
}
