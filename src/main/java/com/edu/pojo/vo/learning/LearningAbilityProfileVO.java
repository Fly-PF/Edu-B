package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Evidence-based ability profile calculated from persisted study records. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningAbilityProfileVO {
    private Integer overallScore;
    private String level;
    private Integer dataConfidence;
    private String summary;
    private String pattern;
    private Integer balanceScore;
    private String dominantDimensionKey;
    private String priorityDimensionKey;
    private List<Dimension> dimensions;
    private List<String> strengths;
    private List<String> gaps;
    private List<String> nextActions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dimension {
        private String key;
        private String label;
        private Integer score;
        private String evidence;
        private String interpretation;
    }
}
