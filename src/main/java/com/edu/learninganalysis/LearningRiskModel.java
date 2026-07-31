package com.edu.learninganalysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A small, inspectable logistic model for learning-risk prioritisation.
 * It deliberately consumes only behaviour recorded by this system.
 */
public class LearningRiskModel {
    private static final double BASELINE = -1.55;

    public LearningRiskResult assess(LearningRiskInput input) {
        double progressGap = clamp((input.courseAverage() - input.progress()) / 55.0);
        double inactivity = input.idleDays() == null ? 1.0 : clamp((input.idleDays() - 1) / 10.0);
        double deadlinePressure = input.deadlineDays() == null ? 0.0 : clamp((9.0 - input.deadlineDays()) / 9.0);
        double incomplete = input.totalChapters() <= 0 ? 0.0
                : clamp((input.totalChapters() - input.finishedChapters()) / (double) input.totalChapters());
        double lowInvestment = clamp((45.0 - input.studyMinutes()) / 45.0);

        double logit = BASELINE
                + 2.05 * progressGap
                + 1.55 * inactivity
                + 1.25 * deadlinePressure
                + 0.75 * incomplete
                + 0.55 * lowInvestment;
        int score = (int) Math.round(100.0 / (1.0 + Math.exp(-logit)));
        String level = score >= 70 ? "HIGH" : score >= 45 ? "MEDIUM" : "LOW";

        List<LearningRiskFactor> factors = new ArrayList<>();
        factors.add(new LearningRiskFactor("progressGap", "进度落后", (int) Math.round(progressGap * 100),
                "当前进度 " + input.progress() + "% ，同班课程均值 " + input.courseAverage() + "%"));
        factors.add(new LearningRiskFactor("inactivity", "学习间隔", (int) Math.round(inactivity * 100),
                input.idleDays() == null ? "还没有可分析的学习记录" : "距上次学习 " + input.idleDays() + " 天"));
        if (input.deadlineDays() != null) {
            factors.add(new LearningRiskFactor("deadline", "截止压力", (int) Math.round(deadlinePressure * 100),
                    "距离截止 " + Math.max(0, input.deadlineDays()) + " 天"));
        }
        factors.add(new LearningRiskFactor("completion", "章节完成", (int) Math.round(incomplete * 100),
                "已完成 " + input.finishedChapters() + " / " + input.totalChapters() + " 个章节"));
        factors.add(new LearningRiskFactor("investment", "学习投入", (int) Math.round(lowInvestment * 100),
                "累计有效学习记录 " + input.studyMinutes() + " 分钟"));
        factors.sort(Comparator.comparing(LearningRiskFactor::weight).reversed());

        int estimatedDays = estimateDays(input, inactivity);
        String recommendation = buildRecommendation(input, factors.getFirst());
        return new LearningRiskResult(score, level, estimatedDays, factors, recommendation, "Logistic regression v1.0");
    }

    private int estimateDays(LearningRiskInput input, double inactivity) {
        int remaining = Math.max(0, 100 - input.progress());
        if (remaining == 0) {
            return 0;
        }
        double dailyProgress = Math.max(4.0, 14.0 - inactivity * 7.0 + Math.min(5.0, input.studyMinutes() / 30.0));
        return Math.max(1, (int) Math.ceil(remaining / dailyProgress));
    }

    private String buildRecommendation(LearningRiskInput input, LearningRiskFactor topFactor) {
        return switch (topFactor.code()) {
            case "inactivity" -> "先完成一个 20 分钟的小节，恢复连续学习记录后再评估。";
            case "deadline" -> "优先完成下一个未完成章节，并在截止前安排一次复盘。";
            case "progressGap" -> "先补当前未完成章节，再用一道小练习确认是否真正掌握。";
            case "investment" -> "把任务拆成 20 分钟，完成后记录具体卡点。";
            default -> input.progress() >= 80 ? "保持当前节奏，完成下一个章节后再复盘。" : "先完成下一个未完成章节，再记录卡点。";
        };
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public record LearningRiskInput(
            int progress,
            int courseAverage,
            Integer idleDays,
            Integer deadlineDays,
            int totalChapters,
            int finishedChapters,
            int studyMinutes
    ) {
    }

    public record LearningRiskFactor(String code, String label, int weight, String evidence) {
    }

    public record LearningRiskResult(
            int score,
            String level,
            int estimatedDays,
            List<LearningRiskFactor> factors,
            String recommendation,
            String modelVersion
    ) {
    }
}
