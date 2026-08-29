package com.edu.pojo.vo.gov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovMockExamRecordVO {
    private Long practiceId;
    private String subject;
    private Integer totalCount;
    private Integer correctCount;
    private BigDecimal score;
    private String status;
    private Integer durationLimitSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}

