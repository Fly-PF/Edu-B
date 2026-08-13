package com.edu.pojo.vo.rag;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagChatMessageVO {
    private String status;

    private Long id;

    private Long sessionId;

    private String messageId;

    private String role;

    private String content;

    private String metadata;

    private Integer docRefCount;

    private List<RagChatDocRefVO> docRefInfo;

    private String createTime;

    private Long reviewRecordId;
}
