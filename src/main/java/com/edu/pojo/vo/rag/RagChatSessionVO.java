package com.edu.pojo.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagChatSessionVO {
    private Long id;

    private String sessionName;

    private Integer kbRefCount;
}
