package com.edu.controller;

import com.edu.ai.client.AiModelClient;
import com.edu.ai.client.MockAiModelClient;
import com.edu.exception.handler.GlobalExceptionHandler;
import com.edu.service.TeacherAiService;
import com.edu.service.impl.TeacherAiServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.math.BigDecimal;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(TeacherAiControllerTest.TestConfig.class)
@WebAppConfiguration
class TeacherAiControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    @WithMockUser(authorities = "TEACHER")
    void teacherGeneratesCompleteLessonPlanWithoutCourseId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/ai/lesson-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLessonPlanRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode data = responseData(result);
        for (String field : new String[]{
                "objectives", "keyPoints", "difficultPoints", "preparations",
                "teachingSteps", "activities", "exercises", "rubric", "notes"
        }) {
            assertTrue(data.path(field).isArray(), field + "应为列表");
            assertFalse(data.path(field).isEmpty(), field + "不能为空");
        }
        int durationTotal = StreamSupport.stream(data.path("teachingSteps").spliterator(), false)
                .mapToInt(step -> step.path("durationMinutes").asInt())
                .sum();
        assertEquals(45, durationTotal);
    }

    @Test
    @WithMockUser(authorities = "STUDENT")
    void studentCannotGenerateLessonPlan() throws Exception {
        mockMvc.perform(post("/api/teacher/ai/lesson-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLessonPlanRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("权限认证失败！"));
    }

    @Test
    @WithMockUser(authorities = "TEACHER")
    void teacherGeneratesGradingThatSatisfiesScoreInvariants() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/ai/gradings/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validGradingRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode data = responseData(result);
        BigDecimal totalScore = data.path("totalScore").decimalValue();
        BigDecimal dimensionTotal = BigDecimal.ZERO;
        for (JsonNode dimension : data.path("dimensionScores")) {
            BigDecimal score = dimension.path("score").decimalValue();
            BigDecimal dimensionMaxScore = dimension.path("maxScore").decimalValue();
            assertTrue(score.compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(score.compareTo(dimensionMaxScore) <= 0);
            dimensionTotal = dimensionTotal.add(score);
        }
        assertEquals(0, dimensionTotal.compareTo(totalScore));
        assertTrue(totalScore.compareTo(new BigDecimal("10.0")) <= 0);

        BigDecimal confidence = data.path("confidence").decimalValue();
        assertTrue(confidence.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(confidence.compareTo(BigDecimal.ONE) <= 0);
    }

    @Test
    @WithMockUser(authorities = "STUDENT")
    void studentCannotGenerateGrading() throws Exception {
        mockMvc.perform(post("/api/teacher/ai/gradings/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validGradingRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("权限认证失败！"));
    }

    @Test
    @WithMockUser(authorities = "TEACHER")
    void invalidRequiredParameterReturnsUnifiedValidationError() throws Exception {
        mockMvc.perform(post("/api/teacher/ai/lesson-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": " ",
                                  "grade": "研一",
                                  "durationMinutes": 45,
                                  "objectives": "理解基本原理",
                                  "difficulty": "进阶"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("课题名称不能为空"));
    }

    @Test
    @WithMockUser(authorities = "TEACHER")
    void gradingRejectsRubricTotalDifferentFromMaxScore() throws Exception {
        mockMvc.perform(post("/api/teacher/ai/gradings/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "说明光合作用的意义",
                                  "questionType": "简答题",
                                  "referenceAnswer": "光合作用将光能转化为化学能，制造有机物并释放氧气。",
                                  "rubric": [
                                    {"criterion": "知识准确性", "description": "概念准确", "maxScore": 4.0},
                                    {"criterion": "要点完整性", "description": "要点完整", "maxScore": 5.0}
                                  ],
                                  "studentAnswer": "光合作用能制造有机物并释放氧气。",
                                  "maxScore": 10.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("评分维度分值之和必须等于总分"));
    }

    @Test
    @WithMockUser(authorities = "TEACHER")
    void gradingRejectsMoreThanOneDecimalPlace() throws Exception {
        mockMvc.perform(post("/api/teacher/ai/gradings/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "说明光合作用的意义",
                                  "questionType": "简答题",
                                  "referenceAnswer": "光合作用制造有机物。",
                                  "rubric": [
                                    {"criterion": "知识准确性", "description": "概念准确", "maxScore": 0.16}
                                  ],
                                  "studentAnswer": "光合作用制造有机物。",
                                  "maxScore": 0.16
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("分值最多保留一位小数"));
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private String validLessonPlanRequest() {
        return """
                {
                  "topic": "线性回归",
                  "grade": "研一",
                  "durationMinutes": 45,
                  "objectives": "理解线性回归的基本原理\\n能够解释模型参数",
                  "difficulty": "进阶",
                  "requirements": "加入生活案例"
                }
                """;
    }

    private String validGradingRequest() {
        return """
                {
                  "question": "说明光合作用的意义",
                  "questionType": "简答题",
                  "referenceAnswer": "光合作用将光能转化为化学能，制造有机物并释放氧气。",
                  "rubric": [
                    {"criterion": "知识准确性", "description": "概念准确", "maxScore": 3.3},
                    {"criterion": "要点完整性", "description": "要点完整", "maxScore": 3.3},
                    {"criterion": "逻辑与表达", "description": "表达清晰", "maxScore": 3.4}
                  ],
                  "studentAnswer": "光合作用能制造有机物并释放氧气，为生物提供能量。",
                  "maxScore": 10.0
                }
                """;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        AiModelClient aiModelClient() {
            return new MockAiModelClient();
        }

        @Bean
        TeacherAiService teacherAiService(AiModelClient aiModelClient) {
            return new TeacherAiServiceImpl(aiModelClient);
        }

        @Bean
        TeacherAiController teacherAiController(TeacherAiService teacherAiService) {
            return new TeacherAiController(teacherAiService);
        }

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }
}
