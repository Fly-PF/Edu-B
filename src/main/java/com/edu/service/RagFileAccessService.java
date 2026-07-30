package com.edu.service;

import com.edu.pojo.vo.rag.RagFilePreviewContentVO;
import com.edu.pojo.vo.rag.RagFilePreviewImagesVO;
import org.springframework.http.ResponseEntity;

public interface RagFileAccessService {
    ResponseEntity<byte[]> getKnowledgeBaseCover(String objectName);

    ResponseEntity<byte[]> getChatImage(String objectName);

    ResponseEntity<byte[]> previewKnowledgeBaseDocument(Long kbId, String fileUrl);

    RagFilePreviewContentVO previewKnowledgeBaseDocumentContent(Long kbId, String fileUrl);

    RagFilePreviewImagesVO previewKnowledgeBaseDocumentImages(Long kbId, String fileUrl);
}
