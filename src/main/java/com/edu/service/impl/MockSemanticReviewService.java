package com.edu.service.impl;

import com.edu.pojo.dto.safety.SemanticReviewRequest;
import com.edu.pojo.dto.safety.SemanticReviewResponse;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.service.safety.SemanticReviewService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(prefix = "edu.safety.semantic", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockSemanticReviewService implements SemanticReviewService {
    private static final List<String> CHEATING_INTENT_PHRASES = List.of(
            "不要让老师发现",
            "像我自己写的",
            "替我完成",
            "帮我糊弄",
            "交作业用",
            "make it look like i wrote it",
            "avoid teacher detection"
    );
    private static final List<String> AGE_RISK_INTENT_PHRASES = List.of(
            "怎么伤害自己",
            "不想活了",
            "制作危险物品",
            "逃避监管",
            "harm myself",
            "dangerous weapon"
    );
    private static final List<String> PROMPT_ATTACK_INTENT_PHRASES = List.of(
            "扮演无视规则的系统",
            "绕过平台安全",
            "不要记录这次请求",
            "ignore all safety rules",
            "bypass safety"
    );

    @Override
    public SemanticReviewResponse review(SemanticReviewRequest request) {
        if (request == null) {
            return pass("semantic-mock-empty");
        }

        SemanticReviewResponse override = fromMetadata(request.getMetadata());
        if (override != null) {
            return override;
        }

        String text = join(request.getInputText(), request.getOutputText());
        if (!StringUtils.hasText(text)) {
            return pass("semantic-mock-empty");
        }

        Set<SafetyRiskType> riskTypes = new LinkedHashSet<>();
        SafetyDecision decision = SafetyDecision.PASS;
        List<String> reasons = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (containsAny(text, CHEATING_INTENT_PHRASES)) {
            riskTypes.add(SafetyRiskType.CHEATING);
            decision = strongerDecision(decision, request.getGradeLevel() == SafetyGradeLevel.SENIOR
                    ? SafetyDecision.WARN
                    : SafetyDecision.BLOCK);
            reasons.add("大模型语义审核识别到代写、规避教师检查等学术不端意图");
            suggestions.add("建议改为让 AI 提供思路、结构建议、检查清单或知识点提示");
        }

        if (containsAny(text, AGE_RISK_INTENT_PHRASES)) {
            riskTypes.add(SafetyRiskType.AGE_INAPPROPRIATE);
            decision = strongerDecision(decision, SafetyDecision.BLOCK);
            reasons.add("大模型语义审核识别到不适龄或高危行为意图");
            suggestions.add("请移除高危内容，并引导学生寻求教师或监护人帮助");
        }

        if (containsAny(text, PROMPT_ATTACK_INTENT_PHRASES)) {
            riskTypes.add(SafetyRiskType.PROMPT_ATTACK);
            decision = strongerDecision(decision, SafetyDecision.BLOCK);
            reasons.add("大模型语义审核识别到绕过平台安全策略的意图");
            suggestions.add("请删除越权、绕过限制或要求隐藏日志的指令");
        }

        if (riskTypes.isEmpty()) {
            return pass("semantic-mock");
        }

        return SemanticReviewResponse.builder()
                .decision(decision)
                .riskLevel(SafetyRiskLevel.HIGH)
                .riskTypes(new ArrayList<>(riskTypes))
                .reason(String.join("；", reasons))
                .suggestion(String.join("；", suggestions))
                .confidence(0.82d)
                .source("semantic-mock")
                .build();
    }

    private SemanticReviewResponse fromMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        SafetyDecision decision = parseEnum(SafetyDecision.class, firstNonBlank(
                metadata.get("semanticDecision"),
                metadata.get("llmDecision")
        ));
        SafetyRiskLevel riskLevel = parseEnum(SafetyRiskLevel.class, firstNonBlank(
                metadata.get("semanticRiskLevel"),
                metadata.get("llmRiskLevel")
        ));
        List<SafetyRiskType> riskTypes = parseRiskTypes(firstNonBlank(
                metadata.get("semanticRiskTypes"),
                metadata.get("llmRiskTypes")
        ));
        if (decision == null && riskLevel == null && riskTypes.isEmpty()) {
            return null;
        }

        SafetyDecision normalizedDecision = decision == null
                ? inferDecisionFromRisk(riskLevel, riskTypes)
                : decision;
        return SemanticReviewResponse.builder()
                .decision(normalizedDecision)
                .riskLevel(riskLevel == null ? inferRiskLevelFromDecision(normalizedDecision) : riskLevel)
                .riskTypes(riskTypes)
                .reason(firstNonBlank(metadata.get("semanticReason"), "来自大模型语义审核元数据的判定结果"))
                .suggestion(firstNonBlank(metadata.get("semanticSuggestion"), "请根据语义审核结果调整内容"))
                .confidence(parseDouble(firstNonBlank(metadata.get("semanticConfidence"), metadata.get("llmConfidence"))))
                .source("metadata")
                .build();
    }

    private SemanticReviewResponse pass(String source) {
        return SemanticReviewResponse.builder()
                .decision(SafetyDecision.PASS)
                .riskLevel(SafetyRiskLevel.LOW)
                .confidence(0.70d)
                .source(source)
                .build();
    }

    private SafetyDecision inferDecisionFromRisk(SafetyRiskLevel riskLevel, List<SafetyRiskType> riskTypes) {
        if (riskLevel == SafetyRiskLevel.HIGH || !riskTypes.isEmpty()) {
            return SafetyDecision.WARN;
        }
        return SafetyDecision.PASS;
    }

    private SafetyRiskLevel inferRiskLevelFromDecision(SafetyDecision decision) {
        if (decision == SafetyDecision.BLOCK
                || decision == SafetyDecision.REWRITE
                || decision == SafetyDecision.DESENSITIZE) {
            return SafetyRiskLevel.HIGH;
        }
        if (decision == SafetyDecision.WARN) {
            return SafetyRiskLevel.MEDIUM;
        }
        return SafetyRiskLevel.LOW;
    }

    private SafetyDecision strongerDecision(SafetyDecision current, SafetyDecision candidate) {
        return decisionWeight(candidate) > decisionWeight(current) ? candidate : current;
    }

    private int decisionWeight(SafetyDecision decision) {
        if (decision == SafetyDecision.BLOCK) {
            return 5;
        }
        if (decision == SafetyDecision.REWRITE) {
            return 4;
        }
        if (decision == SafetyDecision.DESENSITIZE) {
            return 3;
        }
        if (decision == SafetyDecision.WARN) {
            return 2;
        }
        return 1;
    }

    private boolean containsAny(String text, List<String> phrases) {
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String phrase : phrases) {
            if (normalized.contains(phrase.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String join(String first, String second) {
        if (!StringUtils.hasText(first)) {
            return second;
        }
        if (!StringUtils.hasText(second)) {
            return first;
        }
        return first + "\n" + second;
    }

    private List<SafetyRiskType> parseRiskTypes(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }

        List<SafetyRiskType> result = new ArrayList<>();
        for (String item : value.split("[,，、;；\\s]+")) {
            SafetyRiskType riskType = parseEnum(SafetyRiskType.class, item);
            if (riskType != null) {
                result.add(riskType);
            }
        }
        return result;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
