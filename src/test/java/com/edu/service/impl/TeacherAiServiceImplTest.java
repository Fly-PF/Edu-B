package com.edu.service.impl;

import com.edu.ai.client.AiModelClient;
import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingDimensionScore;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanExercise;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanTeachingStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherAiServiceImplTest {

    @Test
    void allowsEmptyDeductionsAndSuggestions() {
        StubClient client = new StubClient(validResponse(true));
        TeacherAiServiceImpl service = new TeacherAiServiceImpl(client);

        assertDoesNotThrow(() -> service.generateGrading(request()));
        assertTrue(client.gradingCalls == 1);
    }

    @Test
    void rejectsMissingStrengths() {
        TeacherAiServiceImpl service = new TeacherAiServiceImpl(new StubClient(validResponse(false)));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generateGrading(request())
        );
        assertTrue(exception.getMessage().contains("答案优点"));
    }

    @Test
    void fillsActivitiesFromTeachingStepsWhenMissing() {
        StubClient client = new StubClient(validResponse(true), lessonPlanWithoutActivities());
        TeacherAiServiceImpl service = new TeacherAiServiceImpl(client);

        LessonPlanGenerateResponse response = service.generateLessonPlan(lessonPlanRequest());

        assertEquals(1, client.lessonPlanCalls);
        assertTrue(response.getActivities() != null && !response.getActivities().isEmpty());
        assertTrue(response.getActivities().get(0).contains("Warm-up"));
        assertTrue(response.getPreparations() != null && !response.getPreparations().isEmpty());
        assertTrue(response.getNotes() != null && !response.getNotes().isEmpty());
    }

    @Test
    void keepsModelActivitiesWhenPresent() {
        StubClient client = new StubClient(validResponse(true), lessonPlanWithActivities());
        TeacherAiServiceImpl service = new TeacherAiServiceImpl(client);

        LessonPlanGenerateResponse response = service.generateLessonPlan(lessonPlanRequest());

        assertEquals(List.of("Model activity"), response.getActivities());
    }

    @Test
    void rejectsLessonPlanWhenTeachingStepsMissing() {
        LessonPlanGenerateResponse response = lessonPlanWithActivities();
        response.setTeachingSteps(List.of());
        TeacherAiServiceImpl service = new TeacherAiServiceImpl(new StubClient(validResponse(true), response));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generateLessonPlan(lessonPlanRequest())
        );

        assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank());
    }

    private GradingGenerateRequest request() {
        GradingGenerateRequest request = new GradingGenerateRequest();
        request.setQuestion("Explain supervised learning.");
        request.setQuestionType("Short answer");
        request.setReferenceAnswer("Supervised learning maps inputs to target outputs.");
        request.setStudentAnswer("It maps inputs to outputs.");
        request.setRubric(List.of(
                rubric("Accuracy", "Correctly explain the mapping.", 5),
                rubric("Completeness", "Cover the core idea.", 5)
        ));
        request.setMaxScore(new BigDecimal("10.0"));
        return request;
    }

    private GradingGenerateResponse validResponse(boolean includeStrengths) {
        return GradingGenerateResponse.builder()
                .totalScore(new BigDecimal("8.0"))
                .dimensionScores(List.of(
                        dimension("Accuracy", "4.0", "5.0", "Correctly mentions the mapping."),
                        dimension("Completeness", "4.0", "5.0", "Covers the core idea.")
                ))
                .strengths(includeStrengths ? List.of("Clear mapping idea.") : null)
                .deductions(List.of())
                .suggestions(List.of())
                .referenceAnswer("Supervised learning maps inputs to target outputs.")
                .revisedAnswer("Supervised learning learns a mapping from inputs to target outputs.")
                .confidence(new BigDecimal("0.9"))
                .build();
    }

    private LessonPlanGenerateRequest lessonPlanRequest() {
        LessonPlanGenerateRequest request = new LessonPlanGenerateRequest();
        request.setTopic("Python basics");
        request.setGrade("Grade 7");
        request.setDurationMinutes(45);
        request.setObjectives("Teach the basics.");
        request.setDifficulty("Medium");
        request.setRequirements("Keep it practical.");
        return request;
    }

    private LessonPlanGenerateResponse lessonPlanWithoutActivities() {
        LessonPlanGenerateResponse response = lessonPlanWithActivities();
        response.setActivities(List.of());
        response.setPreparations(List.of());
        response.setNotes(List.of());
        return response;
    }

    private LessonPlanGenerateResponse lessonPlanWithActivities() {
        return LessonPlanGenerateResponse.builder()
                .title("Lesson title")
                .objectives(List.of("Objective 1"))
                .keyPoints(List.of("Key point 1"))
                .difficultPoints(List.of("Difficult point 1"))
                .preparations(List.of("Prepare 1"))
                .teachingSteps(List.of(
                        LessonPlanTeachingStep.builder()
                                .stage("Warm-up")
                                .durationMinutes(15)
                                .teacherActivity("Explain")
                                .studentActivity("Listen")
                                .purpose("Introduce topic")
                                .build(),
                        LessonPlanTeachingStep.builder()
                                .stage("Practice")
                                .durationMinutes(30)
                                .teacherActivity("Guide")
                                .studentActivity("Practice")
                                .purpose("Reinforce")
                                .build()
                ))
                .activities(List.of("Model activity"))
                .exercises(List.of(
                        LessonPlanExercise.builder()
                                .question("What is Python?")
                                .type("Short answer")
                                .referenceAnswer("A programming language.")
                                .difficulty("Easy")
                                .build()
                ))
                .rubric(List.of(rubric("Accuracy", "Be correct.", 5)))
                .notes(List.of("Note 1"))
                .build();
    }

    private AiRubricItem rubric(String criterion, String description, int maxScore) {
        return AiRubricItem.builder()
                .criterion(criterion)
                .description(description)
                .maxScore(new BigDecimal(maxScore))
                .build();
    }

    private GradingDimensionScore dimension(String criterion, String score, String maxScore, String reason) {
        return GradingDimensionScore.builder()
                .criterion(criterion)
                .score(new BigDecimal(score))
                .maxScore(new BigDecimal(maxScore))
                .reason(reason)
                .build();
    }

    private static final class StubClient implements AiModelClient {
        private final GradingGenerateResponse gradingResponse;
        private final LessonPlanGenerateResponse lessonPlanResponse;
        private int gradingCalls;
        private int lessonPlanCalls;

        private StubClient(GradingGenerateResponse gradingResponse) {
            this(gradingResponse, null);
        }

        private StubClient(
                GradingGenerateResponse gradingResponse,
                LessonPlanGenerateResponse lessonPlanResponse
        ) {
            this.gradingResponse = gradingResponse;
            this.lessonPlanResponse = lessonPlanResponse;
        }

        @Override
        public LessonPlanGenerateResponse generateLessonPlan(LessonPlanGenerateRequest request) {
            lessonPlanCalls++;
            return lessonPlanResponse;
        }

        @Override
        public GradingGenerateResponse generateGrading(GradingGenerateRequest request) {
            gradingCalls++;
            return gradingResponse;
        }
    }
}
