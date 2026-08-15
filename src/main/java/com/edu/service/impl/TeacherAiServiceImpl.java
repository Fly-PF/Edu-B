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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAiServiceImpl implements TeacherAiService {
    private final AiModelClient aiModelClient;

    @Override
    public LessonPlanGenerateResponse generateLessonPlan(LessonPlanGenerateRequest request) {
        long startedAt = System.nanoTime();
        LessonPlanGenerateResponse response = aiModelClient.generateLessonPlan(request);
        long validationStartedAt = System.nanoTime();
        normalizeLessonPlanResponse(response);
        validateLessonPlanResponse(request, response);
        log.info("lesson-plan ai service validation cost={} ms", elapsedMillis(validationStartedAt));
        log.info("lesson-plan ai total cost={} ms", elapsedMillis(startedAt));
        return response;
    }

    @Override
    public GradingGenerateResponse generateGrading(GradingGenerateRequest request) {
        long startedAt = System.nanoTime();
        long requestValidationStartedAt = System.nanoTime();
        BigDecimal rubricTotal = request.getRubric().stream()
                .map(AiRubricItem::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (rubricTotal.compareTo(request.getMaxScore()) != 0) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "评分维度分值之和必须等于总分");
        }
        log.info("grading ai request validation cost={} ms", elapsedMillis(requestValidationStartedAt));
        GradingGenerateResponse response = aiModelClient.generateGrading(request);
        long validationStartedAt = System.nanoTime();
        validateGradingResponse(request, response);
        log.info("grading ai service validation cost={} ms", elapsedMillis(validationStartedAt));
        log.info("grading ai total cost={} ms", elapsedMillis(startedAt));
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

    private void normalizeLessonPlanResponse(LessonPlanGenerateResponse response) {
        if (response == null) {
            return;
        }
        if (response.getActivities() == null || response.getActivities().isEmpty()) {
            response.setActivities(buildActivitiesFromTeachingSteps(response));
        }
        if (response.getPreparations() == null || response.getPreparations().isEmpty()) {
            response.setPreparations(buildPreparations());
        }
        if (response.getNotes() == null || response.getNotes().isEmpty()) {
            response.setNotes(buildNotes(response));
        }
    }

    private List<String> buildActivitiesFromTeachingSteps(LessonPlanGenerateResponse response) {
        if (response.getTeachingSteps() == null || response.getTeachingSteps().isEmpty()) {
            return List.of();
        }
        return response.getTeachingSteps().stream()
                .filter(step -> step != null
                        && StringUtils.hasText(step.getStage())
                        && StringUtils.hasText(step.getStudentActivity()))
                .map(step -> {
                    /*
                    String activity = step.getStage().trim() + "：" + step.getStudentActivity().trim();
                    if (StringUtils.hasText(step.getPurpose())) {
                        return activity + "；目标：" + step.getPurpose().trim();
                    }
                    return activity;
                    */
                    String activity = step.getStage().trim() + ": " + step.getStudentActivity().trim();
                    if (StringUtils.hasText(step.getPurpose())) {
                        return activity + "; purpose: " + step.getPurpose().trim();
                    }
                    return activity;
                })
                .toList();
    }

    private List<String> buildPreparations() {
        /*
        return List.of("教师准备与教学目标对应的讲解材料和课堂任务单。");
    }

        */
        return List.of("Prepare lesson materials and task sheets aligned with the teaching objectives.");
    }

    private List<String> buildNotes(LessonPlanGenerateResponse response) {
        /*
        if (response.getDifficultPoints() == null || response.getDifficultPoints().isEmpty()) {
            return List.of("课堂中根据学生反馈及时调整讲解节奏。");
        }
        return List.of("围绕教学难点及时观察学生反馈，并进行针对性追问。");
    }

        */
        if (response.getDifficultPoints() == null || response.getDifficultPoints().isEmpty()) {
            return List.of("Adjust the teaching pace in time based on student feedback.");
        }
        return List.of("Observe feedback around the difficult points and add targeted follow-up questions.");
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
        requirePresent(response.getDeductions(), "扣分原因");
        requirePresent(response.getSuggestions(), "修改建议");
        if (!StringUtils.hasText(response.getReferenceAnswer())) {
            throw invalidAiResponse("参考答案为空");
        }
        if (response.getRevisedAnswer() == null || response.getRevisedAnswer().isBlank()) {
            throw invalidAiResponse("参考改写答案为空");
        }
        if (response.getConfidence() == null
                || response.getConfidence().compareTo(BigDecimal.ZERO) < 0
                || response.getConfidence().compareTo(BigDecimal.ONE) > 0) {
            throw invalidAiResponse("置信度超出范围");
        }

        if (response.getDimensionScores().size() != request.getRubric().size()) {
            throw invalidAiResponse("分项得分数量与评分标准不一致");
        }
        BigDecimal dimensionTotal = BigDecimal.ZERO;
        for (int index = 0; index < request.getRubric().size(); index++) {
            AiRubricItem expected = request.getRubric().get(index);
            var dimension = response.getDimensionScores().get(index);
            if (dimension == null || dimension.getScore() == null || dimension.getMaxScore() == null
                    || !StringUtils.hasText(dimension.getCriterion())
                    || !StringUtils.hasText(dimension.getReason())) {
                throw invalidAiResponse("分项得分不完整");
            }
            if (!expected.getCriterion().trim().equals(dimension.getCriterion().trim())
                    || expected.getMaxScore().compareTo(dimension.getMaxScore()) != 0) {
                throw invalidAiResponse("分项得分与评分标准未一一对应");
            }
            if (dimension.getScore().compareTo(BigDecimal.ZERO) < 0
                    || dimension.getScore().compareTo(dimension.getMaxScore()) > 0) {
                throw invalidAiResponse("分项得分超出范围");
            }
            if (!hasAtMostOneDecimal(dimension.getScore())) {
                throw invalidAiResponse("分项得分精度不正确");
            }
            dimensionTotal = dimensionTotal.add(dimension.getScore());
        }

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

    private void requirePresent(List<?> values, String fieldName) {
        if (values == null) {
            throw invalidAiResponse(fieldName + "为空");
        }
    }

    private boolean hasAtMostOneDecimal(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 1;
    }

    private IllegalStateException invalidAiResponse(String detail) {
        return new IllegalStateException("AI返回结果不符合约束：" + detail);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
