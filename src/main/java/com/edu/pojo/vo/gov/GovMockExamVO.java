package com.edu.pojo.vo.gov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovMockExamVO {
    private Long practiceId;
    private String status;
    private String subject;
    private Integer totalCount;
    private Integer answeredCount;
    private Integer durationLimitSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<GovMockQuestionVO> questions;
}

