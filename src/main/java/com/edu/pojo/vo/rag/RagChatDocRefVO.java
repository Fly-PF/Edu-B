package com.edu.pojo.vo.rag;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagChatDocRefVO {
    @JsonIgnore
    private Long docId;

    private String kbName;

    private String docName;

    private String contentSource;

    private String fileUrl;
}
