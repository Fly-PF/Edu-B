package com.edu.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentClassCourseDTO {
    private Long assignmentId;
    private Long courseId;
    private String courseName;
    private String cover;
    private String grade;
    private Integer difficulty;
    private Integer courseType;
    private Integer totalDuration;
    private Integer totalChapter;
    private String publishTime;
    private String deadline;
    private Integer studyStatus;
    private Integer progress;
}
