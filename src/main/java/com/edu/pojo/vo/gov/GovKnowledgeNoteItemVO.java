package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GovKnowledgeNoteItemVO {
    private Long noteId;
    private Long knowledgeId;
    private String subject;
    private String nodeType;
    private String title;
    private String noteContent;
    private String notePreview;
    private String progressStatus;
    private LocalDateTime updatedAt;
}
