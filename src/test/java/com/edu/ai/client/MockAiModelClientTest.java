package com.edu.ai.client;

import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockAiModelClientTest {
    private final MockAiModelClient client = new MockAiModelClient();

    @Test
    void lessonPlanUsesTopicGradeAndDuration() {
        LessonPlanGenerateRequest request = new LessonPlanGenerateRequest();
        request.setTopic("二次函数");
        request.setGrade("高中");
        request.setDurationMinutes(50);
        request.setObjectives("理解二次函数图像\n能够分析开口方向和顶点");
        request.setDifficulty("进阶");

        LessonPlanGenerateResponse response = client.generateLessonPlan(request);

        assertTrue(response.getTitle().contains("高中"));
        assertTrue(response.getTitle().contains("二次函数"));
        assertEquals(2, response.getObjectives().size());
        assertEquals(
                50,
                response.getTeachingSteps().stream()
                        .mapToInt(step -> step.getDurationMinutes())
                        .sum()
        );
    }

    @Test
    void gradingChangesWithStudentAnswer() {
        GradingGenerateRequest request = gradingRequest("光合作用能够制造有机物并释放氧气。", 10);
        GradingGenerateResponse completeAnswer = client.generateGrading(request);

        request.setStudentAnswer("释放氧气。");
        GradingGenerateResponse incompleteAnswer = client.generateGrading(request);

        assertNotEquals(completeAnswer.getTotalScore(), incompleteAnswer.getTotalScore());
        assertTrue(completeAnswer.getTotalScore().compareTo(incompleteAnswer.getTotalScore()) > 0);
        assertTrue(completeAnswer.getTotalScore().compareTo(request.getMaxScore()) <= 0);
    }

    @Test
    void gradingKeepsExactScoreInvariantsAndUsesSpecificFeedback() {
        GradingGenerateRequest request = gradingRequest("光合作用能够制造有机物并释放氧气。", 10);

        GradingGenerateResponse response = client.generateGrading(request);

        BigDecimal dimensionTotal = response.getDimensionScores().stream()
                .map(item -> {
                    assertTrue(item.getScore().compareTo(BigDecimal.ZERO) >= 0);
                    assertTrue(item.getScore().compareTo(item.getMaxScore()) <= 0);
                    return item.getScore();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, dimensionTotal.compareTo(response.getTotalScore()));
        assertTrue(response.getTotalScore().compareTo(request.getMaxScore()) <= 0);
        assertTrue(response.getConfidence().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(response.getConfidence().compareTo(BigDecimal.ONE) <= 0);

        long distinctReasons = response.getDimensionScores().stream()
                .map(item -> item.getReason())
                .distinct()
                .count();
        assertEquals(response.getDimensionScores().size(), distinctReasons);
        assertTrue(response.getStrengths().stream().anyMatch(item -> item.contains("制造有机物")));
        assertTrue(response.getStrengths().stream().anyMatch(item -> item.contains("释放氧气")));
        assertFalse(response.getSuggestions().stream().anyMatch(item -> item.contains("逐条对照")));
        assertFalse(response.getRevisedAnswer().contains("。。"));
    }

    @Test
    void gradingDetectsObviousFactualErrorAndCleansRepeatedPunctuation() {
        GradingGenerateRequest request = gradingRequest("光合作用会消耗氧气。", 10);
        request.setReferenceAnswer("光合作用制造有机物，并释放氧气。。");

        GradingGenerateResponse response = client.generateGrading(request);

        assertTrue(response.getDimensionScores().stream()
                .filter(item -> item.getCriterion().contains("知识"))
                .anyMatch(item -> item.getReason().contains("事实性问题")));
        assertTrue(response.getDeductions().stream().anyMatch(item -> item.contains("消耗氧气")));
        assertFalse(response.getRevisedAnswer().contains("。。"));
        assertTrue(response.getRevisedAnswer().endsWith("。"));
    }

    private GradingGenerateRequest gradingRequest(String studentAnswer, int maxScore) {
        GradingGenerateRequest request = new GradingGenerateRequest();
        request.setQuestion("说明光合作用的意义");
        request.setQuestionType("简答题");
        request.setReferenceAnswer("光合作用将光能转化为化学能，制造有机物并释放氧气。");
        request.setRubric(List.of(
                rubric("知识准确性", "核心概念准确", 4),
                rubric("要点完整性", "主要要点完整", 3),
                rubric("逻辑与表达", "语言清晰且结构完整", 3)
        ));
        request.setStudentAnswer(studentAnswer);
        request.setMaxScore(BigDecimal.valueOf(maxScore));
        return request;
    }

    private AiRubricItem rubric(String criterion, String description, int maxScore) {
        return AiRubricItem.builder()
                .criterion(criterion)
                .description(description)
                .maxScore(BigDecimal.valueOf(maxScore))
                .build();
    }
}
