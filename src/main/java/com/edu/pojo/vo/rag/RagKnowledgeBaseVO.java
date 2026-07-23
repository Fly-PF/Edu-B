package com.edu.pojo.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagKnowledgeBaseVO {
    private Long id;

    private String kbName;

    private String kbCover;

    private String description;

    private Integer kbType;

    private Integer publicFlag;

    private Integer status;
}
