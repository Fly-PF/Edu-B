package com.edu.pojo.dto.teacherai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonPlanExercise {
    private String question;
    private String type;
    private String referenceAnswer;
    private String difficulty;
}
