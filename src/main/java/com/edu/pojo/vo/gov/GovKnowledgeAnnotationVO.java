package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GovKnowledgeAnnotationVO {
    private Long annotationId;
    private Long knowledgeId;
    private String subject;
    private String nodeType;
    private String title;
    private String sectionKey;
    private String sectionTitle;
    private Integer startOffset;
    private Integer endOffset;
    private String selectedText;
    private String selectedPreview;
    private String noteContent;
    private String notePreview;
    private String color;
    private String progressStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
