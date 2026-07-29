package com.edu.service;

import java.util.List;
import java.util.Locale;

/** Rules that prevent unsafe or academically dishonest requests reaching a model. */
public final class AiCompanionSafetyPolicy {
    private static final List<String> DANGEROUS_PHRASES = List.of(
            "制作炸弹", "制造爆炸物", "制作毒品", "自制枪", "入侵系统", "盗取密码",
            "绕过登录", "窃取账号"
    );
    private static final List<String> ACADEMIC_DISHONESTY_PHRASES = List.of(
            "代写", "替我写", "替我完成", "帮我做作业", "帮我抄作业", "帮我作弊",
            "考试作弊", "直接给我考试答案", "考试答案", "试卷答案", "绕过查重", "绕过检测"
    );

    private AiCompanionSafetyPolicy() {
    }

    public static SafetyDecision check(String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (ACADEMIC_DISHONESTY_PHRASES.stream().anyMatch(normalized::contains)) {
            return new SafetyDecision(
                    "BLOCKED_ACADEMIC",
                    "我不能代写作业、实验或提供考试答案。你可以把自己的思路、已完成的步骤或不理解的概念发给我，我可以帮你检查方法、解释知识点和设计练习步骤。"
            );
        }
        if (DANGEROUS_PHRASES.stream().anyMatch(normalized::contains)) {
            return new SafetyDecision(
                    "BLOCKED_SAFETY",
                    "这个问题涉及不安全或违法风险，我不能提供具体方法。你可以改为询问课程概念、实验规范或安全学习方法。"
            );
        }
        return new SafetyDecision("NORMAL", null);
    }

    public record SafetyDecision(String status, String response) {
        public boolean blocked() {
            return status != null && status.startsWith("BLOCKED_");
        }
    }
}
