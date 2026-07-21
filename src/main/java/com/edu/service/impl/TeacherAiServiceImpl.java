package com.edu.service.impl;

import com.edu.ai.client.AiModelClient;
import com.edu.exception.UserErrorException;
import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import com.edu.service.TeacherAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherAiServiceImpl implements TeacherAiService {
    private final AiModelClient aiModelClient;

    @Override
    public LessonPlanGenerateResponse generateLessonPlan(LessonPlanGenerateRequest request) {
        LessonPlanGenerateResponse response = aiModelClient.generateLessonPlan(request);
        validateLessonPlanResponse(request, response);
        return response;
    }

    @Override
    public GradingGenerateResponse generateGrading(GradingGenerateRequest request) {
        BigDecimal rubricTotal = request.getRubric().stream()
                .map(AiRubricItem::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (rubricTotal.compareTo(request.getMaxScore()) != 0) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "评分维度分值之和必须等于总分");
        }
        GradingGenerateResponse response = aiModelClient.generateGrading(request);
        validateGradingResponse(request, response);
        return response;
    }

    private void validateLessonPlanResponse(
            LessonPlanGenerateRequest request,
            LessonPlanGenerateResponse response
    ) {
        if (response == null || response.getTitle() == null || response.getTitle().isBlank()) {
            throw invalidAiResponse("教案标题为空");
        }
        requireNonEmpty(response.getObjectives(), "教学目标");
        requireNonEmpty(response.getKeyPoints(), "教学重点");
        requireNonEmpty(response.getDifficultPoints(), "教学难点");
        requireNonEmpty(response.getPreparations(), "教学准备");
        requireNonEmpty(response.getTeachingSteps(), "教学流程");
        requireNonEmpty(response.getActivities(), "课堂活动");
        requireNonEmpty(response.getExercises(), "练习题");
        requireNonEmpty(response.getRubric(), "评分标准");
        requireNonEmpty(response.getNotes(), "教学注意事项");

        int totalDuration = response.getTeachingSteps().stream()
                .mapToInt(step -> {
                    if (step == null || step.getDurationMinutes() == null || step.getDurationMinutes() <= 0) {
                        throw invalidAiResponse("教学步骤时长无效");
                    }
                    return step.getDurationMinutes();
                })
                .sum();
        if (totalDuration != request.getDurationMinutes()) {
            throw invalidAiResponse("教学步骤时长之和与课时不一致");
        }
    }

    private void validateGradingResponse(
            GradingGenerateRequest request,
            GradingGenerateResponse response
    ) {
        if (response == null || response.getTotalScore() == null) {
            throw invalidAiResponse("批改总分为空");
        }
        requireNonEmpty(response.getDimensionScores(), "分项得分");
        requireNonEmpty(response.getStrengths(), "答案优点");
        requireNonEmpty(response.getDeductions(), "扣分原因");
        requireNonEmpty(response.getSuggestions(), "修改建议");
        if (response.getRevisedAnswer() == null || response.getRevisedAnswer().isBlank()) {
            throw invalidAiResponse("参考改写答案为空");
        }
        if (response.getConfidence() == null
                || response.getConfidence().compareTo(BigDecimal.ZERO) < 0
                || response.getConfidence().compareTo(BigDecimal.ONE) > 0) {
            throw invalidAiResponse("置信度超出范围");
        }

        BigDecimal dimensionTotal = response.getDimensionScores().stream()
                .map(dimension -> {
                    if (dimension == null || dimension.getScore() == null || dimension.getMaxScore() == null) {
                        throw invalidAiResponse("分项得分不完整");
                    }
                    if (dimension.getScore().compareTo(BigDecimal.ZERO) < 0
                            || dimension.getScore().compareTo(dimension.getMaxScore()) > 0) {
                        throw invalidAiResponse("分项得分超出范围");
                    }
                    if (!hasAtMostOneDecimal(dimension.getScore())) {
                        throw invalidAiResponse("分项得分精度不正确");
                    }
                    return dimension.getScore();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!hasAtMostOneDecimal(response.getTotalScore())) {
            throw invalidAiResponse("批改总分精度不正确");
        }
        if (dimensionTotal.compareTo(response.getTotalScore()) != 0) {
            throw invalidAiResponse("分项得分之和与批改总分不一致");
        }
        if (response.getTotalScore().compareTo(request.getMaxScore()) > 0) {
            throw invalidAiResponse("批改总分超过请求总分");
        }
    }

    private void requireNonEmpty(List<?> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw invalidAiResponse(fieldName + "为空");
        }
    }

    private boolean hasAtMostOneDecimal(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 1;
    }

    private IllegalStateException invalidAiResponse(String detail) {
        return new IllegalStateException("AI返回结果不符合约束：" + detail);
    }
}
