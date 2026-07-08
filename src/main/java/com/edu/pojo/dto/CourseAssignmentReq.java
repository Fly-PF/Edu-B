package com.edu.pojo.dto;

import lombok.Data;

@Data
public class CourseAssignmentReq {
    private Long courseId;
    private Long classId;
    private String deadline;
}
