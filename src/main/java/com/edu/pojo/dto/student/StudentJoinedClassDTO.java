package com.edu.pojo.dto.student;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StudentJoinedClassDTO {
    private Long classId;
    private String className;
    private Long teacherId;
    private String teacherName;
    private String grade;
    private String school;
    private String classCode;
    private Integer joinType;
    private Integer studentCount;
    private Integer assignedCourseCount;
    private Integer status;
    private LocalDateTime joinTime;
}
