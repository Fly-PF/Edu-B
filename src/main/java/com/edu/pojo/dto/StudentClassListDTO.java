package com.edu.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentClassListDTO {
    private Long id;
    private String className;
    private String school;
    private String grade;
    private String teacherName;
    private Integer studentCount;
    private Integer classStatus;
    private String joinTime;
}
