package com.edu.pojo.vo.gov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovMockExamReportVO {
    private Long practiceId;
    private String subject;
    private String status;
    private Integer totalCount;
    private Integer correctCount;
    private BigDecimal score;
    private BigDecimal accuracyRate;
    private Integer durationUsedSeconds;
    private Integer durationLimitSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<GovMockSubjectBreakdownVO> subjectBreakdown;
    private List<GovWrongQuestionVO> wrongQuestions;
}

