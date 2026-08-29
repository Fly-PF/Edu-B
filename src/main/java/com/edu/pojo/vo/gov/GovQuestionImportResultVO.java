package com.edu.pojo.vo.gov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovQuestionImportResultVO {
    private int totalCount;
    private int successCount;
    private int failedCount;
    private List<GovQuestionImportErrorVO> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GovQuestionImportErrorVO {
        private int index;
        private String reason;
    }
}

