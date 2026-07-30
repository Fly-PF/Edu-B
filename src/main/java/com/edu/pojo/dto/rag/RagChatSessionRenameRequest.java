package com.edu.pojo.dto.rag;

import lombok.Data;

@Data
public class RagChatSessionRenameRequest {
    private Long sessionId;
    private String sessionName;
}
