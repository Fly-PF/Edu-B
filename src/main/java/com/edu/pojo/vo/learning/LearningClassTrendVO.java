package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Seven-day learning activity aggregated from persisted study records. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningClassTrendVO {
    private String period;
    private Integer totalStudyMinutes;
    private Integer activeStudents;
    private List<DailyTrend> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend {
        private String date;
        private Integer studyMinutes;
        private Integer activeStudents;
    }
}
