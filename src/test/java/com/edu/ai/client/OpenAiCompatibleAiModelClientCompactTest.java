package com.edu.ai.client;

import com.edu.common.properties.AiProviderProperties;
import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingDimensionScore;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanExercise;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanTeachingStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleAiModelClientCompactTest {
    private OpenAiCompatibleAiModelClient client;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        client = new OpenAiCompatibleAiModelClient(
                RestClient.builder(),
                new ObjectMapper(),
                AiProviderProperties.builder()
                        .baseUrl("https://example.invalid/v1")
                        .model("unit-test-model")
                        .gradingModel("unit-test-grading-model")
                        .lessonModel("unit-test-lesson-model")
                        .build()
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void compactSystemPromptRequiresCompletenessAndEvidence() {
        String prompt = client.compactGradingSystemPrompt();
        assertTrue(prompt.contains("completeness check"));
        assertTrue(prompt.contains("语义评分"));
        assertTrue(prompt.contains("关键词匹配"));
    }

    @Test
    void compactGradingPromptKeepsFrontendUsedFields() {
        String prompt = client.buildGradingPrompt(gradingRequest());
        assertTrue(prompt.contains("question"));
        assertTrue(prompt.contains("questionType"));
        assertTrue(prompt.contains("studentAnswer"));
        assertTrue(prompt.contains("referenceAnswer"));
        assertTrue(prompt.contains("dimensionScores"));
        assertTrue(prompt.contains("revisedAnswer"));
        assertTrue(prompt.contains("不要输出 referenceAnswer"));
    }

    @Test
    void payloadUsesGradingModelAndBackfillsReferenceAnswer() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = mapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", mapper.writeValueAsString(validResponse()))))
            ));
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        client = new OpenAiCompatibleAiModelClient(
                RestClient.builder(),
                mapper,
                AiProviderProperties.builder()
                        .baseUrl("http://localhost:" + server.getAddress().getPort() + "/v1")
                        .apiKey(UUID.randomUUID().toString())
                        .model("unit-test-model")
                        .gradingModel("unit-test-grading-model")
                        .lessonModel("unit-test-lesson-model")
                        .build()
        );

        GradingGenerateResponse response = client.generateGrading(gradingRequest());
        JsonNode payload = mapper.readTree(requestBody.get());
        assertEquals("unit-test-grading-model", payload.path("model").asText());
        assertEquals("unit-test-grading-model", response.getModel());
        assertEquals(gradingRequest().getReferenceAnswer(), response.getReferenceAnswer());
        assertDoesNotThrow(() -> client.validateGradingResponse(gradingRequest(), response));
    }

    @Test
    void lessonPlanUsesLessonModel() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = mapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", mapper.writeValueAsString(validLessonPlanResponse()))))
            ));
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        client = new OpenAiCompatibleAiModelClient(
                RestClient.builder(),
                mapper,
                AiProviderProperties.builder()
                        .baseUrl("http://localhost:" + server.getAddress().getPort() + "/v1")
                        .apiKey(UUID.randomUUID().toString())
                        .model("unit-test-model")
                        .gradingModel("unit-test-grading-model")
                        .lessonModel("unit-test-lesson-model")
                        .build()
        );

        LessonPlanGenerateResponse response = client.generateLessonPlan(lessonPlanRequest());
        JsonNode payload = mapper.readTree(requestBody.get());
        assertEquals("unit-test-lesson-model", payload.path("model").asText());
        assertEquals(0, payload.path("temperature").decimalValue().compareTo(BigDecimal.ZERO));
        assertEquals("Lesson title", response.getTitle());
        assertEquals(2, response.getTeachingSteps().size());
    }

    private GradingGenerateRequest gradingRequest() {
        GradingGenerateRequest request = new GradingGenerateRequest();
        request.setQuestion("Explain supervised and unsupervised learning.");
        request.setQuestionType("Short answer");
        request.setReferenceAnswer("Supervised learning learns a mapping from inputs to target outputs. Unsupervised learning discovers structure in unlabeled data.");
        request.setStudentAnswer("The model learns the relationship between input and output, and clustering finds hidden structure in unlabeled data.");
        request.setRubric(List.of(
                rubric("Accuracy", "Explain the core distinction correctly.", "5.0"),
                rubric("Completeness", "Cover both the mapping idea and the unlabeled-data idea.", "5.0")
        ));
        request.setMaxScore(new BigDecimal("10.0"));
        return request;
    }

    private GradingGenerateResponse validResponse() {
        return GradingGenerateResponse.builder()
                .totalScore(new BigDecimal("9.0"))
                .dimensionScores(List.of(
                        dimension("Accuracy", "4.5", "5.0", "The answer states that supervised learning learns a mapping between input and output."),
                        dimension("Completeness", "4.5", "5.0", "The answer also mentions discovering hidden structure in unlabeled data.")
                ))
                .strengths(List.of("It correctly treats the input-output relationship as semantic evidence."))
                .deductions(List.of("It could mention that supervised learning uses labeled targets more explicitly."))
                .suggestions(List.of("Add one sentence about labels to make the distinction more complete."))
                .referenceAnswer("Supervised learning learns a mapping from inputs to target outputs. Unsupervised learning discovers structure in unlabeled data.")
                .revisedAnswer("Supervised learning uses labeled examples to learn a mapping from input to target output, while unsupervised learning discovers patterns or structure in unlabeled data.")
                .confidence(new BigDecimal("0.87"))
                .build();
    }

    private LessonPlanGenerateRequest lessonPlanRequest() {
        LessonPlanGenerateRequest request = new LessonPlanGenerateRequest();
        request.setCourseId(1L);
        request.setTopic("Python basics");
        request.setGrade("Grade 7");
        request.setDurationMinutes(45);
        request.setObjectives("Teach the basics.");
        request.setDifficulty("Medium");
        request.setRequirements("Keep it practical.");
        return request;
    }

    private LessonPlanGenerateResponse validLessonPlanResponse() {
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
                .activities(List.of("Activity 1"))
                .exercises(List.of(
                        LessonPlanExercise.builder()
                                .question("What is Python?")
                                .type("Short answer")
                                .referenceAnswer("A programming language.")
                                .difficulty("Easy")
                                .build()
                ))
                .rubric(List.of(rubric("Accuracy", "Be correct.", "5.0")))
                .notes(List.of("Note 1"))
                .build();
    }

    private AiRubricItem rubric(String criterion, String description, String maxScore) {
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
}
