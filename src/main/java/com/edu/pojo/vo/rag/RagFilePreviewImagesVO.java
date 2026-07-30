package com.edu.pojo.vo.rag;

import java.util.List;

public record RagFilePreviewImagesVO(String fileName, String extension, List<RagFilePreviewImageVO> pages) {
}
