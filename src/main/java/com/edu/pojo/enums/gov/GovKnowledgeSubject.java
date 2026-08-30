package com.edu.pojo.enums.gov;

import java.util.Arrays;

public enum GovKnowledgeSubject {
    POLITICAL_THEORY("政治理论"),
    COMMON_SENSE("常识判断"),
    LANGUAGE_AND_EXPRESSION("语言理解与表达"),
    NUMERICAL_RELATIONS("数量关系"),
    JUDGMENT_REASONING("判断推理"),
    DATA_ANALYSIS("资料分析");

    private final String label;

    GovKnowledgeSubject(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static GovKnowledgeSubject resolve(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("科目不能为空");
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(normalized) || item.label.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("科目不正确"));
    }
}
