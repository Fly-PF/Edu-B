package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningStudentTypeProfileVO {
    private Long studentId;
    private String studentName;
    private LearningCourseProfileVO profile;
}
