package com.edu.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CourseMaterialRelevance {
    private static final List<String> QUESTION_NOISE = List.of(
            "请根据当前课程", "请根据当前章节", "课程资料未覆盖", "中文学习包", "课程资料", "学习资料",
            "请解释一下", "请说明一下", "请介绍一下", "请告诉我", "请根据", "当前课程", "当前章节",
            "第一章", "第二章", "第三章", "第四章", "第五章", "第六章", "第七章", "第八章", "第九章",
            "并说明", "请解释", "请说明", "请介绍", "什么是", "是什么", "有哪些", "有那些", "为什么",
            "怎么样", "如何", "主要", "相关", "关于", "内容", "问题", "答案", "本题", "帮我", "请问"
    );
    private static final Set<String> GENERIC_TERMS = Set.of(
            "课程", "章节", "资料", "学习", "解释", "说明", "介绍", "主要", "能力", "应用", "内容",
            "问题", "答案", "当前", "一下", "一种", "哪些", "什么", "怎么", "为什么"
    );

    private CourseMaterialRelevance() {
    }

    public static boolean matches(String question, String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        List<String> terms = extractTerms(question);
        if (terms.isEmpty()) {
            return false;
        }
        String normalizedContent = content.toLowerCase(Locale.ROOT);
        int longestLength = terms.stream().mapToInt(String::length).max().orElse(0);
        int requiredLength = longestLength >= 3 ? longestLength : 2;
        return terms.stream()
                .filter(term -> term.length() >= requiredLength)
                .anyMatch(normalizedContent::contains);
    }

    static List<String> extractTerms(String question) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        for (String noise : QUESTION_NOISE) {
            normalized = normalized.replace(noise, " ");
        }
        normalized = normalized.replaceAll("[的了和与及并中里上下面为是有]", " ");

        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String part : normalized.split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")) {
            String term = part.trim();
            if (term.length() >= 2 && term.length() <= 16 && !GENERIC_TERMS.contains(term)) {
                terms.add(term);
            }
        }
        return terms.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
