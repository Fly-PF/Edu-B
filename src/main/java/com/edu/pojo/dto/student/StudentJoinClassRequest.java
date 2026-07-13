package com.edu.pojo.dto.student;

import lombok.Data;

@Data
public class StudentJoinClassRequest {
    private Long classId;
    private String classCode;
}
