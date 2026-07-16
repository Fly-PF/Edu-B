package com.edu.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherClassListDTO {
    private Long id;
    private String className;
    private String school;
    private String grade;
    private String classCode;
    private Integer joinType;
    private Integer studentCount;
    private Integer classStatus;
    private String createTime;
}
