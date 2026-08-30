package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;
import lombok.Builder.Default;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class GovKnowledgeNodeVO {
    private Long id;
    private String subject;
    private Long parentId;
    private String nodeType;
    private String title;
    private String contentMd;
    private Integer sortOrder;
    private Integer status;
    private String progressStatus;
    private LocalDateTime completedAt;
    private Boolean hasChildren;
    @Default
    private List<GovKnowledgeNodeVO> children = new ArrayList<>();
}
