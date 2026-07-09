package com.edu.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAssignmentDTO {
    private Long id;
    private Long courseId;
    private String courseName;
    private Long classId;
    private String className;
    private String publishTime;
    private String deadline;
}
