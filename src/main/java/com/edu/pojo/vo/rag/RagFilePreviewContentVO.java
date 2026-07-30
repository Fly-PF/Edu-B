package com.edu.pojo.vo.rag;

import com.edu.common.dto.RagTextChunkDTO;

import java.util.List;

public record RagFilePreviewContentVO(String fileName, String extension, List<RagTextChunkDTO> chunks,
                                      String content, String html) {
}
