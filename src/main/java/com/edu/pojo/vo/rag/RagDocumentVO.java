package com.edu.pojo.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocumentVO {
    private Long id;

    private Long kbId;

    private String docName;

    private String docType;

    private String description;

    private String fileUrl;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
