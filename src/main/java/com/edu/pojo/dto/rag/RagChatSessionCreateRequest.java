package com.edu.pojo.dto.rag;

import lombok.Data;

import java.util.List;

@Data
public class RagChatSessionCreateRequest {
    private String sessionName;
    private List<Long> kbIds;
}
