package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GovKnowledgeFavoriteVO {
    private Long knowledgeId;
    private Boolean favorited;
}
