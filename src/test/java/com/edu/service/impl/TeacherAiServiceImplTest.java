package com.edu.service.impl;

import com.edu.ai.client.AiModelClient;
import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingDimensionScore;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

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
        private int gradingCalls;

        private StubClient(GradingGenerateResponse gradingResponse) {
            this.gradingResponse = gradingResponse;
        }

        @Override
        public LessonPlanGenerateResponse generateLessonPlan(LessonPlanGenerateRequest request) {
            return null;
        }

        @Override
        public GradingGenerateResponse generateGrading(GradingGenerateRequest request) {
            gradingCalls++;
            return gradingResponse;
        }
    }
}
