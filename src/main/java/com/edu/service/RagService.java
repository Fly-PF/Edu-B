package com.edu.service;

import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RagService {
    List<RagKnowledgeBaseVO> listMyKnowledgeBases(String keyword, Integer status, Integer isPublic, Integer kbType);

    RagKnowledgeBaseVO getMyKnowledgeBase(Long kbId);

    void createKnowledgeBase(String kbName, String description, Integer kbType, Integer isPublic, MultipartFile file);

    void updateKnowledgeBase(Long kbId, String kbName, String description, Integer kbType, Integer isPublic,
                             Integer status, MultipartFile file);

    void uploadRagFile(HttpServletRequest request, MultipartFile file, String description, Long kbId);

    String chatTest(String message, List<MultipartFile> files);

    float[] embeddingTest(String message);
}
