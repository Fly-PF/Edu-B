package com.edu.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherClassDetailDTO {
    private Long id;
    private String className;
    private String school;
    private String grade;
    private String classCode;
    private Integer joinType;
    private Integer studentCount;
    private Integer classStatus;
    private String createTime;
    private List<StudentBrief> students;
    private List<AssignedCourse> assignedCourses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentBrief {
        private Long studentId;
        private String studentName;
        private String joinTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedCourse {
        private Long courseId;
        private String courseName;
        private String cover;
        private String publishTime;
        private String deadline;
    }
}
