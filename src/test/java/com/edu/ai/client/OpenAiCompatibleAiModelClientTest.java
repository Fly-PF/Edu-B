package com.edu.ai.client;

import com.edu.common.properties.AIModelProperties;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiCompatibleAiModelClientTest {

    @Test
    void shouldPassV1BaseUrlToSpringAiWithoutRewritingIt() throws Exception {
        AtomicReference<String> requestPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            writeChatCompletion(exchange, "{}");
        });
        server.start();
        try {
            AIModelProperties properties = new AIModelProperties();
            AIModelProperties.Model model = properties.getTeacherAi().getChatModel();
            model.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            model.setApiKey("test-key");
            model.setModelName("test-model");

            OpenAiCompatibleAiModelClient client = new OpenAiCompatibleAiModelClient(properties, new ObjectMapper());
            Method callSpringAi = OpenAiCompatibleAiModelClient.class.getDeclaredMethod(
                    "callSpringAi", String.class, String.class, int.class, String.class, boolean.class);
            callSpringAi.setAccessible(true);

            Object result = callSpringAi.invoke(client, "system", "user", 64, "test-model", true);

            assertEquals("{}", result);
            assertEquals("/v1/chat/completions", requestPath.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldDeserializeSchemaCompliantLessonPlan() throws Exception {
        String lessonPlan = """
                {"title":"分数的初步认识","objectives":["理解分数含义"],"keyPoints":["分数表示"],
                "difficultPoints":["单位1"],"preparations":["课件"],"teachingSteps":[
                {"stage":"导入","durationMinutes":20,"teacherActivity":"演示","studentActivity":"观察","purpose":"激发兴趣"},
                {"stage":"练习","durationMinutes":20,"teacherActivity":"指导","studentActivity":"作答","purpose":"巩固知识"}],
                "activities":["折纸活动"],"exercises":[{"question":"涂色表示二分之一","type":"练习","referenceAnswer":"正确涂色","difficulty":"中等"}],
                "rubric":[{"criterion":"理解","description":"正确表示分数","maxScore":100}],"notes":["关注学情"]}
                """.replaceAll("\\s+", "");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> writeChatCompletion(exchange, lessonPlan));
        server.start();
        try {
            OpenAiCompatibleAiModelClient client = clientFor(server);
            LessonPlanGenerateRequest request = new LessonPlanGenerateRequest();
            request.setTopic("分数的初步认识");
            request.setGrade("小学三年级");
            request.setDurationMinutes(40);
            request.setObjectives("理解分数含义");
            request.setDifficulty("中等");

            LessonPlanGenerateResponse response = client.generateLessonPlan(request);

            assertEquals("分数的初步认识", response.getTitle());
            assertEquals(2, response.getTeachingSteps().size());
            assertEquals(40, response.getTeachingSteps().stream()
                    .mapToInt(step -> step.getDurationMinutes()).sum());
        } finally {
            server.stop(0);
        }
    }

    private OpenAiCompatibleAiModelClient clientFor(HttpServer server) {
        AIModelProperties properties = new AIModelProperties();
        AIModelProperties.Model model = properties.getTeacherAi().getChatModel();
        model.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        model.setApiKey("test-key");
        model.setModelName("test-model");
        return new OpenAiCompatibleAiModelClient(properties, new ObjectMapper());
    }

    private void writeChatCompletion(HttpExchange exchange, String content) throws IOException {
        byte[] response = ("{\"id\":\"test\",\"object\":\"chat.completion\",\"created\":1,"
                + "\"model\":\"test-model\",\"choices\":[{\"index\":0,\"message\":{"
                + "\"role\":\"assistant\",\"content\":" + new ObjectMapper().writeValueAsString(content)
                + "},\"finish_reason\":\"stop\"}]}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
