package com.edu.pojo.dto.teacherai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonPlanTeachingStep {
    private String stage;
    private Integer durationMinutes;
    private String teacherActivity;
    private String studentActivity;
    private String purpose;
}
