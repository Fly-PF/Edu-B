package com.edu.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAssignableCourseDTO {
    private Long courseId;
    private String courseName;
    private String cover;
    private String grade;
    private Integer difficulty;
    private Integer courseType;
    private Long teacherId;
    private Integer isPublic;
    private Integer status;
}
