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
public class StudentClassDetailDTO {
    private Long id;
    private String className;
    private String school;
    private String grade;
    private TeacherBrief teacher;
    private Integer studentCount;
    private Integer classStatus;
    private List<AssignedCourseItem> assignedCourses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherBrief {
        private Long teacherId;
        private String teacherName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedCourseItem {
        private Long courseId;
        private String courseName;
        private String cover;
        private String publishTime;
        private String deadline;
        private Integer studyStatus;
        private Integer progress;
    }
}
