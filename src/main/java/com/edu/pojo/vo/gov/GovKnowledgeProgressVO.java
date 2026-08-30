package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GovKnowledgeProgressVO {
    private Long userId;
    private Long knowledgeId;
    private String status;
    private LocalDateTime completedAt;
}
