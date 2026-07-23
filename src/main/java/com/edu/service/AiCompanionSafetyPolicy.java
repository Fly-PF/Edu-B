package com.edu.service;

import java.util.List;
import java.util.Locale;

/** Rules that prevent unsafe or academically dishonest requests reaching a model. */
public final class AiCompanionSafetyPolicy {
    private static final List<String> BLOCKED_PHRASES = List.of(
            "制作炸弹", "制造爆炸物", "制作毒品", "自制枪", "入侵系统", "盗取密码",
            "绕过登录", "窃取账号", "帮我作弊", "考试作弊", "代写作业", "代写论文",
            "帮我抄作业", "直接给我考试答案", "绕过查重", "绕过检测"
    );

    private AiCompanionSafetyPolicy() {
    }

    public static SafetyDecision check(String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        boolean blocked = BLOCKED_PHRASES.stream().anyMatch(normalized::contains);
        return blocked ? new SafetyDecision("BLOCKED") : new SafetyDecision("NORMAL");
    }

    public record SafetyDecision(String status) {
        public boolean blocked() {
            return "BLOCKED".equals(status);
        }
    }
}
