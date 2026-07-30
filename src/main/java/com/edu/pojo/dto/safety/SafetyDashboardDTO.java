package com.edu.pojo.dto.safety;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyDashboardDTO {
    private Long totalRequests;
    private Long todayRequests;
    private Long passCount;
    private Long warnCount;
    private Long blockCount;
    private Long desensitizeCount;
    private Long rewriteCount;
    private Long highRiskCount;
    private Long manualReviewCount;
    private Long reviewPendingCount;
    private Long reviewApprovedCount;
    private Long reviewRejectedCount;
    private Long reviewNotRequiredCount;

    @Builder.Default
    private List<MetricItem> riskTypeDistribution = new ArrayList<>();

    @Builder.Default
    private List<MetricItem> sourceModuleDistribution = new ArrayList<>();

    @Builder.Default
    private List<MetricItem> gradeDistribution = new ArrayList<>();

    @Builder.Default
    private List<TrendItem> dailyTrend = new ArrayList<>();

    @Builder.Default
    private List<SafetyRecordDTO> recentHighRiskRecords = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricItem {
        private String code;
        private String label;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private Long totalCount;
        private Long highRiskCount;
    }
}
