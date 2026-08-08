package com.edu.ai.client;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.edu.common.properties.AiProviderProperties;
import com.edu.exception.UserErrorException;
import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingDimensionScore;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleAiModelClientStabilityTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private OpenAiCompatibleAiModelClient client;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        client = new OpenAiCompatibleAiModelClient(
                RestClient.builder(),
                mapper,
                AiProviderProperties.builder()
                        .provider("openai-compatible")
                        .baseUrl("https://example.invalid/v1")
                        .model("unit-test-model")
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
    void gradingPromptIncludesSchemaAndStrictOutputRequirements() {
        String prompt = client.buildGradingPrompt(gradingRequest());
        assertTrue(prompt.contains("completeness check"));
        assertTrue(prompt.contains("JSON"));
        assertTrue(prompt.contains("\"strengths\": []"));
        assertTrue(prompt.contains("dimensionScores"));
        assertTrue(prompt.contains("revisedAnswer"));
        assertTrue(prompt.contains("referenceAnswer"));
    }

    @Test
    void acceptsResponseWhenDeductionsAndSuggestionsAreEmptyArrays() {
        GradingGenerateResponse response = validResponse();
        response.setDeductions(List.of());
        response.setSuggestions(List.of());

        assertDoesNotThrow(() -> client.validateGradingResponse(gradingRequest(), response));
    }

    @Test
    void rejectsResponseWhenStrengthsAreMissing() {
        GradingGenerateResponse response = validResponse();
        response.setStrengths(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.validateGradingResponse(gradingRequest(), response)
        );
        assertTrue(exception.getMessage().contains("答案优点缺失"));
    }

    @Test
    void rejectsResponseWhenRubricOrderDoesNotMatch() {
        GradingGenerateResponse response = validResponse();
        response.setDimensionScores(List.of(
                response.getDimensionScores().get(1),
                response.getDimensionScores().get(0)
        ));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.validateGradingResponse(gradingRequest(), response)
        );
        assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank());
    }

    @Test
    void parsesMarkdownJsonResponse() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String content = "```json\n" + mapper.writeValueAsString(validResponse()) + "\n```";
            writeCompletion(exchange, content);
        });
        client = liveClient(UUID.randomUUID().toString());

        GradingGenerateResponse response = client.generateGrading(gradingRequest());
        JsonNode payload = mapper.readTree(requestBody.get());

        assertEquals("unit-test-model", payload.path("model").asText());
        assertEquals(0, payload.path("temperature").decimalValue().compareTo(BigDecimal.ZERO));
        assertEquals("json_object", payload.path("response_format").path("type").asText());
        assertEquals("openai-compatible", response.getProvider());
        assertEquals("unit-test-model", response.getModel());
        assertEquals(1, requestCount.get());
    }

    @Test
    void parsesJsonSurroundedByExplanatoryText() throws Exception {
        startServer(exchange -> writeCompletion(
                exchange,
                "下面是评分结果，请直接使用。\n" + mapper.writeValueAsString(validResponse()) + "\n以上。"
        ));
        client = liveClient(UUID.randomUUID().toString());

        GradingGenerateResponse response = client.generateGrading(gradingRequest());

        assertEquals(new BigDecimal("8.5"), response.getTotalScore());
        assertEquals(2, response.getDimensionScores().size());
    }

    @Test
    void parsesContentWrappedAsJsonString() throws Exception {
        String wrappedContent = mapper.writeValueAsString(mapper.writeValueAsString(validResponse()));
        startServer(exchange -> writeCompletion(exchange, wrappedContent));
        client = liveClient(UUID.randomUUID().toString());

        GradingGenerateResponse response = client.generateGrading(gradingRequest());

        assertEquals(new BigDecimal("8.5"), response.getTotalScore());
    }

    @Test
    void calibrationCaseAStaysInHighScoreBand() throws Exception {
        startServer(exchange -> writeCompletion(exchange, mapper.writeValueAsString(excellentCalibrationResponse())));
        client = liveClient(UUID.randomUUID().toString());

        GradingGenerateResponse response = client.generateGrading(excellentCalibrationRequest());

        assertTrue(response.getTotalScore().compareTo(new BigDecimal("9.5")) >= 0);
        assertTrue(response.getConfidence().compareTo(new BigDecimal("0.85")) >= 0);
    }

    @Test
    void calibrationCaseBDoesNotReachFullMarks() throws Exception {
        startServer(exchange -> writeCompletion(exchange, mapper.writeValueAsString(middleCalibrationResponse())));
        client = liveClient(UUID.randomUUID().toString());

        GradingGenerateResponse response = client.generateGrading(middleCalibrationRequest());

        assertTrue(response.getTotalScore().compareTo(new BigDecimal("10.0")) < 0);
        assertTrue(response.getTotalScore().compareTo(new BigDecimal("8.0")) >= 0);
        assertTrue(response.getTotalScore().compareTo(new BigDecimal("9.1")) <= 0);
    }

    @Test
    void failsAfterTwoInvalidJsonResponses() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeCompletion(exchange, "{invalid json");
        });
        client = liveClient(UUID.randomUUID().toString());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.generateGrading(gradingRequest())
        );

        assertEquals(2, requestCount.get());
        assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank());
    }

    @Test
    void retriesOnceWhenFirstResponseIsInvalidAndSecondResponseIsFixed() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        startServer(exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (requestCount.getAndIncrement() == 0) {
                writeCompletion(exchange, "{\"totalScore\":8.5}");
                return;
            }
            writeCompletion(exchange, mapper.writeValueAsString(validResponse()));
        });
        client = liveClient(UUID.randomUUID().toString());

        GradingGenerateResponse response = client.generateGrading(gradingRequest());

        assertEquals(2, requestCount.get());
        assertEquals(new BigDecimal("8.5"), response.getTotalScore());
        assertTrue(requestBodies.get(1).contains("schema"));
    }

    @Test
    void failsWhenBothAttemptsReturnRubricMismatch() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            GradingGenerateResponse response = validResponse();
            response.setDimensionScores(List.of(
                    dimension("Completeness", "4.0", "5.0", "Mentions unlabeled data."),
                    dimension("Accuracy", "4.5", "5.0", "Explains the mapping relationship.")
            ));
            writeCompletion(exchange, mapper.writeValueAsString(response));
        });
        client = liveClient(UUID.randomUUID().toString());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.generateGrading(gradingRequest())
        );

        assertEquals(2, requestCount.get());
        assertTrue(exception.getMessage().contains("dimensionScores rubric mismatch"));
    }

    @Test
    void doesNotRetryWhenProviderReturns503() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeRawResponse(exchange, 503, "{\"error\":{\"message\":\"service unavailable\"}}");
        });
        client = liveClient(UUID.randomUUID().toString());

        UserErrorException exception = assertThrows(
                UserErrorException.class,
                () -> client.generateGrading(gradingRequest())
        );

        assertEquals(1, requestCount.get());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("AI 服务暂时不可用，请稍后重试。", exception.getMessage());
    }

    @Test
    void doesNotRetryWhenProviderReturns429() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeRawResponse(exchange, 429, "{\"error\":{\"message\":\"rate limit\"}}");
        });
        client = liveClient(UUID.randomUUID().toString());

        UserErrorException exception = assertThrows(
                UserErrorException.class,
                () -> client.generateGrading(gradingRequest())
        );

        assertEquals(1, requestCount.get());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
        assertEquals("AI 服务请求繁忙，请稍后重试。", exception.getMessage());
    }

    @Test
    void classifiesProvider401WithoutRetry() throws Exception {
        assertProviderAuthFailure(401);
    }

    @Test
    void classifiesProvider403WithoutRetry() throws Exception {
        assertProviderAuthFailure(403);
    }

    @Test
    void logsDoNotContainApiKeyOnParseFailure() throws Exception {
        String apiKey = "sk-test-api-key-123456";
        startServer(exchange -> writeCompletion(exchange, "{invalid json"));
        client = liveClient(apiKey);

        Logger logger = (Logger) LoggerFactory.getLogger(OpenAiCompatibleAiModelClient.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThrows(IllegalStateException.class, () -> client.generateGrading(gradingRequest()));
        } finally {
            logger.detachAppender(appender);
        }

        String logs = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertTrue(logs.contains("provider=openai-compatible"));
        assertTrue(logs.contains("httpStatus=200"));
        assertFalse(logs.contains(apiKey));
        assertFalse(logs.contains("Authorization"));
    }

    @Test
    void cacheHitSkipsSecondModelCall() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeCompletion(exchange, mapper.writeValueAsString(validResponse()));
        });
        client = liveClient(UUID.randomUUID().toString());

        GradingGenerateResponse first = client.generateGrading(gradingRequest());
        GradingGenerateResponse second = client.generateGrading(gradingRequest());

        assertEquals(1, requestCount.get());
        assertEquals(first.getTotalScore(), second.getTotalScore());
        assertEquals(first.getReferenceAnswer(), second.getReferenceAnswer());
    }

    @Test
    void cacheMissWhenStudentAnswerChanges() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeCompletion(exchange, mapper.writeValueAsString(validResponse()));
        });
        client = liveClient(UUID.randomUUID().toString());

        GradingGenerateRequest first = gradingRequest();
        GradingGenerateRequest second = gradingRequest();
        second.setStudentAnswer("A different answer with different evidence.");

        client.generateGrading(first);
        client.generateGrading(second);

        assertEquals(2, requestCount.get());
    }

    @Test
    void cacheMissWhenRubricChanges() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeCompletion(exchange, mapper.writeValueAsString(validResponse()));
        });
        client = liveClient(UUID.randomUUID().toString());

        GradingGenerateRequest first = gradingRequest();
        GradingGenerateRequest second = gradingRequest();
        second.getRubric().get(0).setDescription("A different rubric description.");

        client.generateGrading(first);
        client.generateGrading(second);

        assertEquals(2, requestCount.get());
    }

    @Test
    void cacheMissWhenModelChanges() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeCompletion(exchange, mapper.writeValueAsString(validResponse()));
        });

        client = liveClient(UUID.randomUUID().toString(), "unit-test-grading-model-a");
        client.generateGrading(gradingRequest());
        client = liveClient(UUID.randomUUID().toString(), "unit-test-grading-model-b");
        client.generateGrading(gradingRequest());

        assertEquals(2, requestCount.get());
    }

    @Test
    void doesNotCache503Responses() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeRawResponse(exchange, 503, "{\"error\":{\"message\":\"service unavailable\"}}");
        });
        client = liveClient(UUID.randomUUID().toString());

        assertThrows(UserErrorException.class, () -> client.generateGrading(gradingRequest()));
        assertThrows(UserErrorException.class, () -> client.generateGrading(gradingRequest()));

        assertEquals(2, requestCount.get());
    }

    @Test
    void doesNotCacheInvalidJsonResponses() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeCompletion(exchange, "{invalid json");
        });
        client = liveClient(UUID.randomUUID().toString());

        assertThrows(IllegalStateException.class, () -> client.generateGrading(gradingRequest()));
        assertThrows(IllegalStateException.class, () -> client.generateGrading(gradingRequest()));

        assertEquals(4, requestCount.get());
    }

    private void startServer(HttpHandler handler) throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                handler.handle(exchange);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        server.start();
    }

    private void assertProviderAuthFailure(int httpStatus) throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeRawResponse(exchange, httpStatus, "{\"error\":{\"message\":\"forbidden\"}}");
        });
        client = liveClient(UUID.randomUUID().toString());

        UserErrorException exception = assertThrows(
                UserErrorException.class,
                () -> client.generateGrading(gradingRequest())
        );

        assertEquals(1, requestCount.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("AI 服务认证或权限异常，请联系管理员。", exception.getMessage());
    }

    private void writeCompletion(HttpExchange exchange, String content) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", content)))
        ));
        writeRawResponse(exchange, 200, body);
    }

    private void writeRawResponse(HttpExchange exchange, int statusCode, String body) throws Exception {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private OpenAiCompatibleAiModelClient liveClient(String apiKey) {
        return liveClient(apiKey, "unit-test-model");
    }

    private OpenAiCompatibleAiModelClient liveClient(String apiKey, String gradingModel) {
        return new OpenAiCompatibleAiModelClient(
                RestClient.builder(),
                mapper,
                AiProviderProperties.builder()
                        .provider("openai-compatible")
                        .baseUrl("http://localhost:" + server.getAddress().getPort() + "/v1")
                        .apiKey(apiKey)
                        .model("unit-test-model")
                        .gradingModel(gradingModel)
                        .lessonModel("unit-test-lesson-model")
                        .build()
        );
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

    private GradingGenerateRequest excellentCalibrationRequest() {
        GradingGenerateRequest request = new GradingGenerateRequest();
        request.setQuestion("解释监督学习和无监督学习的区别，并分别说明应用场景。");
        request.setQuestionType("简答题");
        request.setReferenceAnswer("监督学习使用已经标注的数据进行训练，模型学习输入和输出之间的关系，适合分类和回归任务，例如根据历史邮件判断新邮件是不是垃圾邮件。无监督学习使用没有人工标签的数据，主要让模型自己发现数据中的结构和相似性，例如根据用户购买记录把用户分成不同群体。");
        request.setStudentAnswer("监督学习使用已经标注的数据进行训练，模型通过样本学习输入和输出之间的关系，适合分类和回归任务，例如根据历史邮件判断新邮件是不是垃圾邮件。无监督学习使用没有人工标签的数据，主要让模型自己发现数据中的结构和相似性，例如根据用户购买记录把用户分成不同群体。");
        request.setRubric(List.of(
                rubric("概念准确性", "准确说明两类学习方法的核心区别", "4.0"),
                rubric("区别完整性", "说明输入输出映射、分类回归、结构发现、聚类等关键差异", "3.0"),
                rubric("案例匹配性", "监督学习和无监督学习案例归类正确", "2.0"),
                rubric("表达清晰度", "表达清楚并能用证据支持结论", "1.0")
        ));
        request.setMaxScore(new BigDecimal("10.0"));
        return request;
    }

    private GradingGenerateRequest middleCalibrationRequest() {
        GradingGenerateRequest request = new GradingGenerateRequest();
        request.setQuestion("解释监督学习和无监督学习的区别，并分别说明应用场景。");
        request.setQuestionType("简答题");
        request.setReferenceAnswer("监督学习使用已经标注的数据进行训练，模型学习输入和输出之间的关系，适合分类和回归任务，例如根据历史邮件判断新邮件是不是垃圾邮件。无监督学习使用没有人工标签的数据，主要让模型自己发现数据中的结构和相似性，例如根据用户购买记录把用户分成不同群体。");
        request.setStudentAnswer("监督学习是用有标签的数据训练模型，例如垃圾邮件分类。无监督学习是不使用标签的数据，让模型自己去找数据中的规律，例如把用户分成不同群体。两者主要区别就是一个有标签，一个没有标签。");
        request.setRubric(List.of(
                rubric("概念准确性", "准确说明两类学习方法的核心区别", "4.0"),
                rubric("区别完整性", "说明输入输出映射、分类回归、结构发现、聚类等关键差异", "3.0"),
                rubric("案例匹配性", "监督学习和无监督学习案例归类正确", "2.0"),
                rubric("表达清晰度", "表达清楚并能用证据支持结论", "1.0")
        ));
        request.setMaxScore(new BigDecimal("10.0"));
        return request;
    }

    private GradingGenerateResponse validResponse() {
        return GradingGenerateResponse.builder()
                .totalScore(new BigDecimal("8.5"))
                .dimensionScores(List.of(
                        dimension("Accuracy", "4.5", "5.0", "The answer states that supervised learning learns a mapping between input and output."),
                        dimension("Completeness", "4.0", "5.0", "The answer also mentions discovering hidden structure in unlabeled data.")
                ))
                .strengths(List.of("It correctly treats the input-output relationship as semantic evidence of supervised learning."))
                .deductions(List.of("It could mention that supervised learning uses labeled targets more explicitly."))
                .suggestions(List.of("Add one sentence about labels to make the distinction more complete."))
                .referenceAnswer("Supervised learning learns a mapping from inputs to target outputs. Unsupervised learning discovers structure in unlabeled data.")
                .revisedAnswer("Supervised learning uses labeled examples to learn a mapping from input to target output, while unsupervised learning discovers patterns or structure in unlabeled data.")
                .confidence(new BigDecimal("0.87"))
                .build();
    }

    private GradingGenerateResponse excellentCalibrationResponse() {
        return GradingGenerateResponse.builder()
                .totalScore(new BigDecimal("9.6"))
                .dimensionScores(List.of(
                        dimension("概念准确性", "3.9", "4.0", "The answer correctly explains labeled data, the input-output mapping, and common uses."),
                        dimension("区别完整性", "2.9", "3.0", "The answer covers mapping, classification/regression, and structure discovery."),
                        dimension("案例匹配性", "2.0", "2.0", "The examples are correctly matched."),
                        dimension("表达清晰度", "0.8", "1.0", "The explanation is clear and evidence-based.")
                ))
                .strengths(List.of("核心概念、差异和案例都覆盖得很完整。"))
                .deductions(List.of("可以再压缩表达，让对比更精炼。"))
                .suggestions(List.of("把两类方法的差异再用一句话总结一下。"))
                .referenceAnswer("参考答案。")
                .revisedAnswer("监督学习使用标注数据学习输入到输出的映射，常用于分类和回归；无监督学习从无标签数据中发现结构，常用于聚类和降维。")
                .confidence(new BigDecimal("0.91"))
                .build();
    }

    private GradingGenerateResponse middleCalibrationResponse() {
        return GradingGenerateResponse.builder()
                .totalScore(new BigDecimal("8.6"))
                .dimensionScores(List.of(
                        dimension("概念准确性", "3.3", "4.0", "The answer correctly identifies labeled and unlabeled data."),
                        dimension("区别完整性", "2.3", "3.0", "The answer misses mapping, classification/regression, and clustering/dimensionality reduction."),
                        dimension("案例匹配性", "2.0", "2.0", "The examples are correctly matched."),
                        dimension("表达清晰度", "1.0", "1.0", "The answer is clear.")
                ))
                .strengths(List.of("基本概念和案例匹配正确。"))
                .deductions(List.of("区别完整性还有重要知识点缺失。"))
                .suggestions(List.of("补充输入输出映射、分类回归、聚类和降维。"))
                .referenceAnswer("参考答案。")
                .revisedAnswer("监督学习用标注数据学习输入输出映射，常用于分类和回归；无监督学习从无标签数据中发现结构，常用于聚类和降维。")
                .confidence(new BigDecimal("0.89"))
                .build();
    }

    @Test
    void listTuplePromptRequiresCompleteCoverageBeforeFullMarks() {
        String prompt = client.buildGradingPrompt(listTupleRequest("The answer explains the main difference and gives one example."));

        assertTrue(prompt.contains("completeness check"));
        assertTrue(prompt.contains("referenceAnswer"));
        assertTrue(prompt.contains("revisedAnswer"));
    }

    @Test
    void listTupleExcellentFixtureStaysInHighBand() {
        GradingGenerateResponse response = listTupleResponse(
                "9.6",
                List.of(
                        dimension("Concept accuracy", "3.0", "3.0", "Both are ordered sequences."),
                        dimension("Core difference", "3.0", "3.0", "List is mutable; tuple is usually immutable."),
                        dimension("Scenario matching", "2.8", "3.0", "The scenarios match the data structures."),
                        dimension("Clarity", "0.8", "1.0", "The answer is clear.")
                ),
                "List and tuple are both ordered sequences; list is mutable and tuple is usually immutable.",
                "List and tuple are both ordered sequences; list is mutable and tuple is usually immutable.",
                "0.93"
        );

        assertDoesNotThrow(() -> client.validateGradingResponse(listTupleRequest(
                "List and tuple are both ordered sequences; list is mutable and tuple is usually immutable."
        ), response));
        assertTrue(response.getTotalScore().compareTo(new BigDecimal("9.5")) >= 0);
        assertTrue(response.getTotalScore().compareTo(new BigDecimal("10.0")) < 0);
    }

    @Test
    void listTupleMiddleFixtureStaysInExpectedBand() {
        GradingGenerateResponse response = listTupleResponse(
                "8.6",
                List.of(
                        dimension("Concept accuracy", "2.7", "3.0", "The answer is conceptually correct."),
                        dimension("Core difference", "2.4", "3.0", "The answer explains mutability."),
                        dimension("Scenario matching", "2.5", "3.0", "The answer gives one appropriate scenario for each."),
                        dimension("Clarity", "1.0", "1.0", "The answer is clear.")
                ),
                "List and tuple are both ordered sequences, but list is mutable and tuple is usually immutable.",
                "List and tuple are both ordered sequences, but list is mutable and tuple is usually immutable.",
                "0.89"
        );

        assertDoesNotThrow(() -> client.validateGradingResponse(listTupleRequest(
                "List and tuple are both ordered sequences, but list is mutable and tuple is usually immutable."
        ), response));
        assertTrue(response.getTotalScore().compareTo(new BigDecimal("7.5")) >= 0);
        assertTrue(response.getTotalScore().compareTo(new BigDecimal("9.1")) <= 0);
        assertTrue(response.getTotalScore().compareTo(new BigDecimal("10.0")) < 0);
    }

    @Test
    void listTupleWeakFixtureDoesNotGetHighScore() {
        GradingGenerateResponse response = listTupleResponse(
                "5.4",
                List.of(
                        dimension("Concept accuracy", "1.5", "3.0", "Only the broad idea is mentioned."),
                        dimension("Core difference", "1.3", "3.0", "Mutability is only partially explained."),
                        dimension("Scenario matching", "1.6", "3.0", "Only one scenario is given."),
                        dimension("Clarity", "1.0", "1.0", "The answer is understandable.")
                ),
                "Both can store many items, and a list can be changed.",
                "Both can store many items, and a list can be changed.",
                "0.74"
        );

        assertDoesNotThrow(() -> client.validateGradingResponse(listTupleRequest(
                "Both can store many items, and a list can be changed."
        ), response));
        assertTrue(response.getTotalScore().compareTo(new BigDecimal("7.0")) < 0);
    }

    @Test
    void listTupleSemanticEquivalentFixtureStaysHigh() {
        GradingGenerateResponse response = listTupleResponse(
                "9.7",
                List.of(
                        dimension("Concept accuracy", "3.0", "3.0", "Both are ordered sequences."),
                        dimension("Core difference", "3.0", "3.0", "The answer correctly explains mutability."),
                        dimension("Scenario matching", "2.9", "3.0", "The scenarios are matched well."),
                        dimension("Clarity", "0.8", "1.0", "The explanation is organized.")
                ),
                "The student paraphrases the reference answer without copying it, but keeps the meaning complete.",
                "The student paraphrases the reference answer without copying it, but keeps the meaning complete.",
                "0.92"
        );

        assertDoesNotThrow(() -> client.validateGradingResponse(listTupleRequest(
                "The student paraphrases the reference answer without copying it, but keeps the meaning complete."
        ), response));
        assertTrue(response.getTotalScore().compareTo(new BigDecimal("9.5")) >= 0);
    }

    private GradingGenerateRequest listTupleRequest(String studentAnswer) {
        GradingGenerateRequest request = new GradingGenerateRequest();
        request.setQuestion("Explain the main differences between Python lists and tuples and when to use each.");
        request.setQuestionType("Short answer");
        request.setReferenceAnswer("Lists and tuples are both ordered sequences. Lists are mutable, while tuples are usually immutable. Lists fit dynamic data; tuples fit fixed data.");
        request.setStudentAnswer(studentAnswer);
        request.setRubric(List.of(
                rubric("Concept accuracy", "Explain that lists and tuples are both ordered sequences and avoid conceptual errors.", "3.0"),
                rubric("Core difference", "Explain mutability, immutability, and how adding, removing, or changing items differs.", "3.0"),
                rubric("Scenario matching", "Give matching scenarios for both list and tuple, with dynamic vs fixed data.", "3.0"),
                rubric("Clarity", "The answer is clear and logically organized.", "1.0")
        ));
        request.setMaxScore(new BigDecimal("10.0"));
        return request;
    }

    private GradingGenerateResponse listTupleResponse(
            String totalScore,
            List<GradingDimensionScore> dimensions,
            String referenceAnswer,
            String revisedAnswer,
            String confidence
    ) {
        return GradingGenerateResponse.builder()
                .totalScore(new BigDecimal(totalScore))
                .dimensionScores(dimensions)
                .strengths(List.of("Clear explanation of the distinction."))
                .deductions(List.of("Could mention a bit more detail."))
                .suggestions(List.of("Add one more concrete comparison."))
                .referenceAnswer(referenceAnswer)
                .revisedAnswer(revisedAnswer)
                .confidence(new BigDecimal(confidence))
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

    @FunctionalInterface
    private interface HttpHandler {
        void handle(HttpExchange exchange) throws Exception;
    }
}
