package com.edu.pojo.dto.teacherai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonPlanGenerateResponse {
    private String title;
    private List<String> objectives;
    private List<String> keyPoints;
    private List<String> difficultPoints;
    private List<String> preparations;
    private List<LessonPlanTeachingStep> teachingSteps;
    private List<String> activities;
    private List<LessonPlanExercise> exercises;
    private List<AiRubricItem> rubric;
    private List<String> notes;
}
