package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GovKnowledgeFavoriteItemVO {
    private Long favoriteId;
    private Long knowledgeId;
    private String subject;
    private String nodeType;
    private String title;
    private String contentPreview;
    private String progressStatus;
    private LocalDateTime favoritedAt;
}
