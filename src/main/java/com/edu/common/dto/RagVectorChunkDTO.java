package com.edu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagVectorChunkDTO {
    private Long kbId;
    private Long docId;
    private String sourceInfo;
    private String content;
    private float[] vector;
}
