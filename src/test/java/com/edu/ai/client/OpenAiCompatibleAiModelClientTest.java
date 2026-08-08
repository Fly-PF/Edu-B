package com.edu.ai.client;

import com.edu.common.properties.AiProviderProperties;
import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingDimensionScore;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Replaced by compact prompt and cache focused tests.")
class OpenAiCompatibleAiModelClientTest {
    private OpenAiCompatibleAiModelClient client;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        AiProviderProperties properties = AiProviderProperties.builder()
                .baseUrl("https://example.invalid/v1")
                .model("unit-test-model")
                .build();
        client = new OpenAiCompatibleAiModelClient(
                RestClient.builder(),
                new ObjectMapper(),
                properties
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void promptTreatsInputOutputRelationshipAsSemanticEquivalent() {
        String prompt = client.gradingSystemPrompt();
        assertTrue(prompt.contains("输入和输出之间的关系"));
        assertTrue(prompt.contains("输入与目标输出之间的映射关系"));
        assertTrue(prompt.contains("应视为语义等价"));
    }

    @Test
    void promptTreatsClassificationRegressionPhrasesAsSemanticEquivalent() {
        String prompt = client.gradingSystemPrompt();
        assertTrue(prompt.contains("适合分类和回归"));
        assertTrue(prompt.contains("常用于分类和回归"));
        assertTrue(prompt.contains("JSON"));
    }

    @Test
    void promptClassifiesSpamAsSupervisedLearningExample() {
        assertTrue(client.gradingSystemPrompt().contains("垃圾邮件分类属于监督学习"));
    }

    @Test
    void promptClassifiesUserSegmentationAsUnsupervisedLearningExample() {
        assertTrue(client.gradingSystemPrompt().contains("用户分群属于无监督学习"));
    }

    @Test
    void gradingPromptContainsAllRequestFieldsAndFullRubric() {
        String prompt = client.buildGradingPrompt(gradingRequest());
        assertTrue(prompt.contains("question"));
        assertTrue(prompt.contains("questionType"));
        assertTrue(prompt.contains("referenceAnswer"));
        assertTrue(prompt.contains("studentAnswer"));
        assertTrue(prompt.contains("criterion"));
        assertTrue(prompt.contains("description"));
        assertTrue(prompt.contains("maxScore"));
        assertTrue(prompt.contains("completeness check"));
        assertTrue(prompt.contains("full marks"));
        assertTrue(prompt.contains("100%=主要要求全部满足"));
        assertTrue(prompt.contains("满分检查"));
    }

    @Test
    void acceptsResponseWhenDimensionScoresEqualTotalScore() {
        assertDoesNotThrow(() -> client.validateGradingResponse(gradingRequest(), validResponse()));
    }

    @Test
    void rejectsResponseWhenRequiredFieldIsMissing() {
        GradingGenerateResponse response = validResponse();
        response.setDeductions(null);
        assertThrows(IllegalStateException.class, () -> client.validateGradingResponse(gradingRequest(), response));
    }

    @Test
    void rejectsResponseWhenDimensionScoreExceedsMaximum() {
        GradingGenerateResponse response = validResponse();
        response.getDimensionScores().get(0).setScore(new BigDecimal("4.1"));
        assertThrows(IllegalStateException.class, () -> client.validateGradingResponse(gradingRequest(), response));
    }

    @Test
    void sendsOpenAiCompatiblePayloadAndParsesJsonCodeFence() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String content = "```json\n" + mapper.writeValueAsString(validResponse()) + "\n```";
            String body = mapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", content)))
            ));
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiProviderProperties properties = AiProviderProperties.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort() + "/v1")
                .apiKey(UUID.randomUUID().toString())
                .model("unit-test-model")
                .build();
        client = new OpenAiCompatibleAiModelClient(RestClient.builder(), mapper, properties);

        GradingGenerateResponse response = client.generateGrading(gradingRequest());
        JsonNode payload = mapper.readTree(requestBody.get());
        assertTrue(payload.path("model").asText().equals("unit-test-model"));
        assertTrue(payload.path("temperature").decimalValue().compareTo(BigDecimal.ZERO) == 0);
        assertTrue(payload.path("messages").isArray());
        assertTrue(payload.path("messages").size() == 2);
        assertTrue(payload.path("response_format").path("type").asText().equals("json_object"));
        assertTrue(payload.path("max_tokens").asInt() > 0);
        assertTrue(response.getProvider().equals("openai-compatible"));
        assertTrue(response.getModel().equals("unit-test-model"));
    }

    private GradingGenerateRequest gradingRequest() {
        GradingGenerateRequest request = new GradingGenerateRequest();
        request.setQuestion("解释监督学习和无监督学习的区别，并分别说明应用场景。");
        request.setQuestionType("简答题");
        request.setReferenceAnswer("监督学习学习输入与目标输出之间的映射关系，常用于分类和回归；无监督学习发现数据内部结构，可用于用户分群。");
        request.setStudentAnswer("模型通过样本学习输入和输出之间的关系，适合分类和回归任务，例如垃圾邮件分类；另一类模型自己发现数据中的结构和相似性，例如用户分群。");
        request.setRubric(List.of(
                rubric("知识准确性", "准确说明两类学习方法的核心区别", "4.0"),
                rubric("案例匹配性", "监督学习和无监督学习案例归类正确", "3.0"),
                rubric("逻辑与表达", "表达清楚并能用证据支持结论", "3.0")
        ));
        request.setMaxScore(new BigDecimal("10.0"));
        return request;
    }

    private GradingGenerateResponse validResponse() {
        return GradingGenerateResponse.builder()
                .totalScore(new BigDecimal("9.0"))
                .dimensionScores(List.of(
                        dimension("知识准确性", "3.5", "4.0", "答案说明了输入与输出关系，并区分了两类学习方式。"),
                        dimension("案例匹配性", "3.0", "3.0", "垃圾邮件分类和用户分群分别匹配监督学习与无监督学习。"),
                        dimension("逻辑与表达", "2.5", "3.0", "答案结构清晰，但还可以补充两类方法的数据标签差异。")
                ))
                .strengths(List.of("核心概念和案例归类正确。"))
                .deductions(List.of("对标签数据的作用说明可以更明确。"))
                .suggestions(List.of("补充监督学习依赖标签数据的说明。"))
                .referenceAnswer("监督学习与无监督学习参考答案。")
                .revisedAnswer("监督学习利用带标签数据学习映射关系，无监督学习从无标签数据中发现结构。")
                .confidence(new BigDecimal("0.88"))
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
