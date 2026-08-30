package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GovKnowledgeNoteVO {
    private Long knowledgeId;
    private String content;
    private LocalDateTime updatedAt;
}
