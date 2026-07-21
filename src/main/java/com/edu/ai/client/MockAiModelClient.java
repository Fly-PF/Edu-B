package com.edu.ai.client;

import com.edu.pojo.dto.teacherai.AiRubricItem;
import com.edu.pojo.dto.teacherai.GradingDimensionScore;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanExercise;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanTeachingStep;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "edu.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiModelClient implements AiModelClient {
    @Override
    public LessonPlanGenerateResponse generateLessonPlan(LessonPlanGenerateRequest request) {
        String topic = request.getTopic().trim();
        String grade = request.getGrade().trim();
        String difficulty = request.getDifficulty().trim();
        List<String> objectives = splitObjectives(request.getObjectives());
        String primaryObjective = objectives.get(0);
        int[] stageDurations = allocateStageDurations(request.getDurationMinutes());

        List<LessonPlanTeachingStep> teachingSteps = List.of(
                teachingStep(
                        "情境导入",
                        stageDurations[0],
                        "围绕“" + topic + "”展示贴近" + grade + "学习经验的问题情境，提出本课核心问题。",
                        "观察情境并独立记录已有认识，随后用一句话表达初步判断。",
                        "激活先备知识，形成学习期待。"
                ),
                teachingStep(
                        "概念建构",
                        stageDurations[1],
                        "结合示例拆解“" + topic + "”的关键概念，围绕目标“" + primaryObjective + "”进行追问。",
                        "根据示例提取关键信息，小组补充概念要点并修正原有理解。",
                        "建立清晰、可迁移的知识结构。"
                ),
                teachingStep(
                        "合作探究",
                        stageDurations[2],
                        "提供一组由浅入深的任务，巡视并对共性障碍给予分层提示。",
                        "分组完成任务、说明推理过程，并对其他小组方案进行质疑或补充。",
                        "通过表达与互评突破本课难点。"
                ),
                teachingStep(
                        "迁移练习",
                        stageDurations[3],
                        "发布与“" + topic + "”相关的变式练习，根据完成情况进行即时反馈。",
                        "独立作答后对照评分要点自评，再针对错误完成二次订正。",
                        "检验目标达成度并促进方法迁移。"
                ),
                teachingStep(
                        "总结评价",
                        stageDurations[4],
                        "引导学生用关键词回顾学习路径，结合表现性任务给出评价。",
                        "完成学习小结与出口卡，写出一个已掌握要点和一个待解决问题。",
                        "沉淀学习成果，为后续教学提供依据。"
                )
        );

        List<String> preparations = new ArrayList<>(List.of(
                "教师：准备“" + topic + "”示例、分层任务单和课堂评价记录表。",
                "学生：复习与本课目标相关的先备知识，准备记录与展示材料。",
                "环境：确认展示设备可用，并按合作探究需要安排学习小组。"
        ));
        if (request.getCourseId() != null) {
            preparations.add("课程关联：本教案关联课程 ID " + request.getCourseId() + "，可在课程资源中补充配套材料。");
        }

        List<String> notes = new ArrayList<>();
        notes.add(difficultyNote(difficulty));
        notes.add("全程使用目标“" + primaryObjective + "”检查活动与评价是否一致。");
        if (StringUtils.hasText(request.getRequirements())) {
            notes.add("补充要求：" + request.getRequirements().trim());
        } else {
            notes.add("根据课堂反馈灵活调整合作探究与迁移练习的时间分配。");
        }

        return LessonPlanGenerateResponse.builder()
                .title(grade + "《" + topic + "》" + request.getDurationMinutes() + "分钟教学设计")
                .objectives(objectives)
                .keyPoints(List.of(
                        "理解“" + topic + "”的核心概念及其适用条件。",
                        "能够围绕“" + primaryObjective + "”完成解释、分析或应用任务。"
                ))
                .difficultPoints(List.of(
                        "在新情境中选择并说明与“" + topic + "”相关的解决策略。",
                        "以符合“" + difficulty + "”要求的完整过程表达思考依据。"
                ))
                .preparations(preparations)
                .teachingSteps(teachingSteps)
                .activities(List.of(
                        "一分钟观点：学生用关键词写下对“" + topic + "”的初步认识并同伴互换。",
                        "任务拼图：各组分别解决一个子问题，重组后共同形成完整方案。",
                        "出口卡：用“我会了、我还疑惑、我能应用”三个句式完成课堂反馈。"
                ))
                .exercises(buildExercises(topic, grade, difficulty, primaryObjective))
                .rubric(List.of(
                        rubric("知识理解", "概念准确，能说明“" + topic + "”的关键特征。", 35),
                        rubric("方法应用", "能选择恰当方法完成任务，并呈现必要过程。", 40),
                        rubric("表达反思", "表达清晰、有依据，能够根据反馈修正答案。", 25)
                ))
                .notes(notes)
                .build();
    }

    @Override
    public GradingGenerateResponse generateGrading(GradingGenerateRequest request) {
        String studentAnswer = request.getStudentAnswer().trim();
        String referenceAnswer = request.getReferenceAnswer().trim();
        GradingAnalysis analysis = analyzeAnswer(referenceAnswer, studentAnswer);

        List<GradingDimensionScore> dimensions = new ArrayList<>();
        for (AiRubricItem item : request.getRubric()) {
            CriterionCategory category = classifyCriterion(item.getCriterion());
            double ratio = criterionRatio(category, analysis, studentAnswer);
            BigDecimal score = calculateDimensionScore(item.getMaxScore(), ratio);
            dimensions.add(GradingDimensionScore.builder()
                    .criterion(item.getCriterion().trim())
                    .score(score)
                    .maxScore(item.getMaxScore())
                    .reason(dimensionReason(category, item, analysis, studentAnswer, ratio))
                    .build());
        }

        capDimensionScoresToTotal(dimensions, request.getMaxScore());
        BigDecimal totalScore = sumDimensionScores(dimensions);

        List<String> strengths = buildStrengths(analysis, studentAnswer);
        List<String> deductions = buildDeductions(analysis);
        List<String> suggestions = buildSuggestions(analysis, request.getQuestionType());
        double confidence = clamp(
                0.72 + Math.min(0.1, request.getRubric().size() * 0.02)
                        + Math.min(0.08, referenceAnswer.length() / 1000.0)
                        + (studentAnswer.length() >= 10 ? 0.03 : 0),
                0.72,
                0.95
        );

        return GradingGenerateResponse.builder()
                .totalScore(totalScore)
                .dimensionScores(dimensions)
                .strengths(strengths)
                .deductions(deductions)
                .suggestions(suggestions)
                .revisedAnswer(buildRevisedAnswer(referenceAnswer))
                .confidence(BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private List<String> splitObjectives(String text) {
        List<String> objectives = Arrays.stream(text.split("[\\r\\n；;。]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(6)
                .toList();
        return objectives.isEmpty() ? List.of(text.trim()) : objectives;
    }

    private int[] allocateStageDurations(int duration) {
        int introduction = Math.max(2, duration * 10 / 100);
        int construction = Math.max(2, duration * 30 / 100);
        int exploration = Math.max(2, duration * 25 / 100);
        int practice = Math.max(2, duration * 25 / 100);
        int summary = duration - introduction - construction - exploration - practice;
        return new int[]{introduction, construction, exploration, practice, summary};
    }

    private LessonPlanTeachingStep teachingStep(
            String stage,
            int duration,
            String teacherActivity,
            String studentActivity,
            String purpose
    ) {
        return LessonPlanTeachingStep.builder()
                .stage(stage)
                .durationMinutes(duration)
                .teacherActivity(teacherActivity)
                .studentActivity(studentActivity)
                .purpose(purpose)
                .build();
    }

    private List<LessonPlanExercise> buildExercises(
            String topic,
            String grade,
            String difficulty,
            String objective
    ) {
        return List.of(
                LessonPlanExercise.builder()
                        .question("请用自己的语言概括“" + topic + "”的核心含义，并列出两个关键特征。")
                        .type("简答题")
                        .referenceAnswer("应准确说明“" + topic + "”的核心概念，并结合课堂内容列出两个具有区分度的特征。")
                        .difficulty("基础")
                        .build(),
                LessonPlanExercise.builder()
                        .question("面向" + grade + "学习者，设计一个可以应用“" + topic + "”解决的真实情境，并说明步骤。")
                        .type("应用题")
                        .referenceAnswer("情境应真实且条件完整；步骤需体现概念识别、方法选择、过程说明和结果检验。")
                        .difficulty(difficulty)
                        .build(),
                LessonPlanExercise.builder()
                        .question("围绕教学目标“" + objective + "”，分析一种常见错误并给出修正建议。")
                        .type("分析题")
                        .referenceAnswer("指出错误表现及产生原因，使用本课核心知识解释，并给出可执行的修正方法。")
                        .difficulty("挑战")
                        .build()
        );
    }

    private AiRubricItem rubric(String criterion, String description, int maxScore) {
        return AiRubricItem.builder()
                .criterion(criterion)
                .description(description)
                .maxScore(BigDecimal.valueOf(maxScore))
                .build();
    }

    private String difficultyNote(String difficulty) {
        if (difficulty.contains("高") || difficulty.contains("挑战")) {
            return "当前难度为“" + difficulty + "”，需为基础薄弱学生提供步骤卡，同时为进度较快学生准备开放任务。";
        }
        if (difficulty.contains("入门") || difficulty.contains("基础") || difficulty.contains("简单")) {
            return "当前难度为“" + difficulty + "”，应优先使用直观示例并及时确认基础概念。";
        }
        return "当前难度为“" + difficulty + "”，注意在方法示范后保留足够的独立应用时间。";
    }

    private double characterCoverage(String reference, String student) {
        Set<Character> referenceCharacters = meaningfulCharacters(reference);
        Set<Character> studentCharacters = meaningfulCharacters(student);
        if (referenceCharacters.isEmpty()) {
            return 0;
        }
        long matched = referenceCharacters.stream().filter(studentCharacters::contains).count();
        return (double) matched / referenceCharacters.size();
    }

    private Set<Character> meaningfulCharacters(String text) {
        Set<Character> characters = new LinkedHashSet<>();
        for (char character : text.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(character)) {
                characters.add(character);
            }
        }
        return characters;
    }

    private GradingAnalysis analyzeAnswer(String referenceAnswer, String studentAnswer) {
        List<String> keyPoints = extractKeyPoints(referenceAnswer);
        List<String> coveredPoints = keyPoints.stream()
                .filter(point -> isPointCovered(point, studentAnswer))
                .toList();
        List<String> missingPoints = keyPoints.stream()
                .filter(point -> !coveredPoints.contains(point))
                .toList();
        List<String> factualErrors = detectFactualErrors(referenceAnswer, studentAnswer, keyPoints);
        boolean hasStructureWords = containsAny(studentAnswer, "首先", "其次", "最后", "因为", "所以", "因此", "一方面", "另一方面");
        boolean hasTerminalPunctuation = studentAnswer.matches(".*[。！？.!?]$");
        int clauseCount = studentAnswer.split("[，,；;。！？!?\\r\\n]+").length;
        boolean hasLogicJump = studentAnswer.length() >= 35 && clauseCount <= 1 && !hasStructureWords;
        double keyPointCoverage = (double) coveredPoints.size() / Math.max(1, keyPoints.size());
        return new GradingAnalysis(
                keyPoints,
                coveredPoints,
                missingPoints,
                factualErrors,
                hasStructureWords,
                hasTerminalPunctuation,
                hasLogicJump,
                keyPointCoverage
        );
    }

    private List<String> extractKeyPoints(String referenceAnswer) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        Arrays.stream(referenceAnswer.split("[，,；;。！？!?\\r\\n]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(clause -> Arrays.stream(clause.split("并且|以及|同时|并|且"))
                        .map(String::trim)
                        .filter(part -> part.length() >= 2)
                        .forEach(points::add));
        if (points.isEmpty()) {
            points.add(referenceAnswer.trim());
        }
        return points.stream().limit(8).toList();
    }

    private boolean isPointCovered(String point, String studentAnswer) {
        String normalizedPoint = normalizeForMatch(point);
        String normalizedStudent = normalizeForMatch(studentAnswer);
        return normalizedStudent.contains(normalizedPoint)
                || characterCoverage(point, studentAnswer) >= 0.78;
    }

    private String normalizeForMatch(String value) {
        return value.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private List<String> detectFactualErrors(
            String referenceAnswer,
            String studentAnswer,
            List<String> keyPoints
    ) {
        LinkedHashSet<String> errors = new LinkedHashSet<>();
        if (referenceAnswer.contains("释放氧气") && studentAnswer.contains("消耗氧气")) {
            errors.add("将“释放氧气”错误表述为“消耗氧气”");
        }
        if (referenceAnswer.contains("制造有机物") && studentAnswer.contains("消耗有机物")) {
            errors.add("将“制造有机物”错误表述为“消耗有机物”");
        }
        if (referenceAnswer.contains("光能转化为化学能") && studentAnswer.contains("光能转化为热能")) {
            errors.add("将“光能转化为化学能”错误表述为“光能转化为热能”");
        }

        for (String action : List.of("释放", "制造", "产生", "吸收", "促进", "增加", "转化")) {
            if (referenceAnswer.contains(action)
                    && !containsNegatedAction(referenceAnswer, action)
                    && containsNegatedAction(studentAnswer, action)) {
                String relatedPoint = keyPoints.stream()
                        .filter(point -> point.contains(action))
                        .findFirst()
                        .map(this::displayPoint)
                        .orElse(action);
                errors.add("把“" + relatedPoint + "”表述成了否定结论");
            }
        }
        return errors.stream().limit(3).toList();
    }

    private boolean containsNegatedAction(String value, String action) {
        return containsAny(value, "不" + action, "不会" + action, "不能" + action, "未" + action, "没有" + action);
    }

    private CriterionCategory classifyCriterion(String criterion) {
        String normalized = criterion == null ? "" : criterion.trim();
        if (containsAny(normalized, "准确", "正确", "知识", "事实")) {
            return CriterionCategory.KNOWLEDGE_ACCURACY;
        }
        if (containsAny(normalized, "完整", "要点", "覆盖")) {
            return CriterionCategory.COMPLETENESS;
        }
        if (containsAny(normalized, "逻辑", "表达", "结构", "规范", "语言")) {
            return CriterionCategory.LOGIC_AND_EXPRESSION;
        }
        return CriterionCategory.GENERAL;
    }

    private double criterionRatio(
            CriterionCategory category,
            GradingAnalysis analysis,
            String studentAnswer
    ) {
        return switch (category) {
            case KNOWLEDGE_ACCURACY -> {
                if (!analysis.factualErrors().isEmpty()) {
                    yield clamp(0.58 - analysis.factualErrors().size() * 0.1 + analysis.keyPointCoverage() * 0.08, 0.2, 0.62);
                }
                if (analysis.coveredPoints().isEmpty()) {
                    yield 0.35;
                }
                yield clamp(0.7 + analysis.keyPointCoverage() * 0.27, 0.65, 0.97);
            }
            case COMPLETENESS -> clamp(0.2 + analysis.keyPointCoverage() * 0.76, 0.2, 0.96);
            case LOGIC_AND_EXPRESSION -> {
                double ratio = 0.56;
                ratio += analysis.hasTerminalPunctuation() ? 0.1 : 0;
                ratio += analysis.hasStructureWords() ? 0.14 : 0;
                ratio += studentAnswer.length() >= 15 ? 0.1 : 0;
                ratio -= analysis.hasLogicJump() ? 0.12 : 0;
                yield clamp(ratio, 0.35, 0.95);
            }
            case GENERAL -> clamp(0.38 + analysis.keyPointCoverage() * 0.5, 0.3, 0.92);
        };
    }

    private BigDecimal calculateDimensionScore(BigDecimal maxScore, double ratio) {
        BigDecimal zero = BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY);
        BigDecimal scoreCap = maxScore.setScale(1, RoundingMode.DOWN).max(zero);
        BigDecimal score = maxScore
                .multiply(BigDecimal.valueOf(clamp(ratio, 0, 1)))
                .setScale(1, RoundingMode.HALF_UP);
        return score.max(zero).min(scoreCap);
    }

    private void capDimensionScoresToTotal(
            List<GradingDimensionScore> dimensions,
            BigDecimal requestMaxScore
    ) {
        BigDecimal totalCap = requestMaxScore.setScale(1, RoundingMode.DOWN);
        BigDecimal excess = sumDimensionScores(dimensions).subtract(totalCap);
        for (int index = dimensions.size() - 1; index >= 0 && excess.compareTo(BigDecimal.ZERO) > 0; index--) {
            GradingDimensionScore dimension = dimensions.get(index);
            BigDecimal deduction = dimension.getScore().min(excess);
            dimension.setScore(dimension.getScore().subtract(deduction).setScale(1, RoundingMode.UNNECESSARY));
            excess = excess.subtract(deduction);
        }
    }

    private BigDecimal sumDimensionScores(List<GradingDimensionScore> dimensions) {
        return dimensions.stream()
                .map(GradingDimensionScore::getScore)
                .reduce(BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY), BigDecimal::add)
                .setScale(1, RoundingMode.UNNECESSARY);
    }

    private String dimensionReason(
            CriterionCategory category,
            AiRubricItem item,
            GradingAnalysis analysis,
            String studentAnswer,
            double ratio
    ) {
        return switch (category) {
            case KNOWLEDGE_ACCURACY -> knowledgeAccuracyReason(analysis);
            case COMPLETENESS -> completenessReason(analysis);
            case LOGIC_AND_EXPRESSION -> logicAndExpressionReason(analysis, studentAnswer);
            case GENERAL -> generalReason(item, ratio);
        };
    }

    private String knowledgeAccuracyReason(GradingAnalysis analysis) {
        if (!analysis.factualErrors().isEmpty()) {
            return ensureTerminalPunctuation("存在事实性问题：" + String.join("；", analysis.factualErrors()));
        }
        if (analysis.coveredPoints().isEmpty()) {
            return "未呈现与参考答案一致的核心知识表述，暂无法确认关键概念是否掌握。";
        }
        if (analysis.missingPoints().isEmpty()) {
            return "核心概念和事实表述准确，未发现与参考答案冲突的内容。";
        }
        return "核心表述正确，已说明" + quotePoints(analysis.coveredPoints(), 2)
                + "，但缺少" + quotePoints(analysis.missingPoints(), 2) + "。";
    }

    private String completenessReason(GradingAnalysis analysis) {
        if (analysis.missingPoints().isEmpty()) {
            return "参考答案中的关键内容均已覆盖，包括" + quotePoints(analysis.coveredPoints(), 3) + "。";
        }
        if (analysis.coveredPoints().isEmpty()) {
            return "参考答案中的主要得分点尚未覆盖，需补充" + quotePoints(analysis.missingPoints(), 3) + "。";
        }
        return "已覆盖" + quotePoints(analysis.coveredPoints(), 3)
                + "；未明确说明" + quotePoints(analysis.missingPoints(), 3) + "。";
    }

    private String logicAndExpressionReason(GradingAnalysis analysis, String studentAnswer) {
        if (studentAnswer.length() < 10) {
            return "表达过于简略，句意可以理解，但尚未形成完整的说明结构。";
        }
        if (analysis.hasLogicJump()) {
            return "语言基本可读，但多个结论连续出现，缺少连接或解释，存在逻辑跳跃。";
        }
        if (analysis.hasStructureWords() && analysis.hasTerminalPunctuation()) {
            return "语言清晰，使用连接词组织了说明过程，句子结构完整。";
        }
        if (analysis.hasTerminalPunctuation()) {
            return "语言简洁且句意清楚，但各要点之间的关系还可以表达得更明确。";
        }
        return "主要意思能够识别，但句末和层次标记不完整，影响表达的规范性。";
    }

    private String generalReason(AiRubricItem item, double ratio) {
        String level = ratio >= 0.8 ? "完成较好" : ratio >= 0.6 ? "基本达到要求" : "体现不足";
        return "“" + item.getCriterion().trim() + "”" + level + "，评分依据为："
                + ensureTerminalPunctuation(truncate(item.getDescription(), 70));
    }

    private List<String> buildStrengths(GradingAnalysis analysis, String studentAnswer) {
        List<String> strengths = new ArrayList<>();
        analysis.coveredPoints().stream()
                .limit(3)
                .forEach(point -> strengths.add("答案明确写到了“" + displayPoint(point) + "”。"));
        if (!analysis.coveredPoints().isEmpty() && analysis.factualErrors().isEmpty()) {
            strengths.add("上述内容与参考答案方向一致，未发现明显事实错误。");
        }
        if (analysis.hasStructureWords()) {
            strengths.add("使用了连接词组织思路，回答层次较清楚。");
        } else if (analysis.hasTerminalPunctuation() && studentAnswer.length() >= 10) {
            strengths.add("语言简洁，句意完整，能够直接回应题目。");
        }
        if (strengths.isEmpty()) {
            strengths.add("回答直接回应了题目，已具备继续补充的基础：" + truncate(studentAnswer, 35) + "。");
        }
        return strengths.stream().distinct().limit(4).toList();
    }

    private List<String> buildDeductions(GradingAnalysis analysis) {
        List<String> deductions = new ArrayList<>();
        analysis.factualErrors().forEach(error -> deductions.add("事实错误：" + ensureTerminalPunctuation(error)));
        analysis.missingPoints().stream()
                .limit(3)
                .forEach(point -> deductions.add("缺少要点：“" + displayPoint(point) + "”。"));
        if (analysis.hasLogicJump()) {
            deductions.add("多个结论之间缺少解释或连接，逻辑衔接不完整。");
        }
        if (deductions.isEmpty()) {
            deductions.add("未发现明显扣分点，仅可进一步精炼措辞。");
        }
        return deductions.stream().distinct().limit(4).toList();
    }

    private List<String> buildSuggestions(GradingAnalysis analysis, String questionType) {
        List<String> suggestions = new ArrayList<>();
        if (!analysis.factualErrors().isEmpty()) {
            suggestions.add("先修正事实性表述，再检查结论是否与参考答案一致。");
        }
        analysis.missingPoints().stream()
                .limit(2)
                .forEach(point -> suggestions.add(
                        "补充“" + displayPoint(point) + "”，并说明它与题目结论的关系。"
                ));
        if (analysis.hasLogicJump()) {
            suggestions.add("在相邻结论之间加入原因或结果说明，避免直接跳到结论。");
        } else if (!analysis.hasStructureWords() && analysis.keyPoints().size() > 1) {
            suggestions.add("可按“核心结论—关键过程—实际意义”的顺序组织这道" + questionType + "。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("保留现有关键内容，再用一句总结说明其意义，使答案更完整。");
        }
        return suggestions.stream().distinct().limit(4).toList();
    }

    private String buildRevisedAnswer(String referenceAnswer) {
        return ensureTerminalPunctuation(normalizePunctuation(referenceAnswer));
    }

    private String normalizePunctuation(String value) {
        return value.trim()
                .replaceAll("[\\t\\r\\n]+", " ")
                .replaceAll(" {2,}", " ")
                .replaceAll("。+", "。")
                .replace("。.", "。")
                .replace(".。", "。")
                .replaceAll("[，,；;]+[。.!！?？]", "。")
                .replaceAll("[；;]+$", "");
    }

    private String ensureTerminalPunctuation(String value) {
        String normalized = normalizePunctuationWithoutRecursion(value);
        return normalized.matches(".*[。！？.!?]$") ? normalized : normalized + "。";
    }

    private String normalizePunctuationWithoutRecursion(String value) {
        return value.trim()
                .replaceAll("。+", "。")
                .replace("。.", "。")
                .replace(".。", "。");
    }

    private String quotePoints(List<String> points, int limit) {
        return points.stream()
                .limit(limit)
                .map(point -> "“" + displayPoint(point) + "”")
                .reduce((left, right) -> left + "、" + right)
                .orElse("相关要点");
    }

    private String displayPoint(String point) {
        String normalized = point.trim();
        int subjectMarker = normalized.indexOf('将');
        if (subjectMarker > 0 && subjectMarker < normalized.length() - 2) {
            normalized = normalized.substring(subjectMarker + 1);
        }
        return truncate(normalized, 36);
    }

    private boolean containsAny(String value, String... keywords) {
        return Arrays.stream(keywords).anyMatch(value::contains);
    }

    private String truncate(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "…";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum CriterionCategory {
        KNOWLEDGE_ACCURACY,
        COMPLETENESS,
        LOGIC_AND_EXPRESSION,
        GENERAL
    }

    private record GradingAnalysis(
            List<String> keyPoints,
            List<String> coveredPoints,
            List<String> missingPoints,
            List<String> factualErrors,
            boolean hasStructureWords,
            boolean hasTerminalPunctuation,
            boolean hasLogicJump,
            double keyPointCoverage
    ) {
    }
}
