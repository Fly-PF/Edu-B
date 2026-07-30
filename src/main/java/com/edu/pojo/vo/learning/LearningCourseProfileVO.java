package com.edu.pojo.vo.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Aggregated from persisted course study records, never from client-side samples. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningCourseProfileVO {
    private Integer totalStudyMinutes;
    private String dominantType;
    private String insight;
    private List<TypeShare> shares;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeShare {
        private String categoryKey;
        private String typeName;
        private Integer studyMinutes;
        private Integer courseCount;
        private Integer share;
    }
}
