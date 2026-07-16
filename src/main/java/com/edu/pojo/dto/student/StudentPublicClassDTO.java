package com.edu.pojo.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPublicClassDTO {
    private Long classId;
    private String className;
    private Long teacherId;
    private String teacherName;
    private String grade;
    private String school;
    private Integer joinType;
    private Integer studentCount;
    private Integer assignedCourseCount;
    private Integer status;
}
