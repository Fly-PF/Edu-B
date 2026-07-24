package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import com.edu.pojo.vo.rag.RagDocumentVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RagService {
    List<RagKnowledgeBaseVO> listMyKnowledgeBases(String keyword, Integer status, Integer isPublic, Integer kbType);

    List<RagKnowledgeBaseVO> listPublicKnowledgeBases(Integer kbType, Integer limit);

    PageResult<RagKnowledgeBaseVO> pagePublicKnowledgeBases(String keyword, Integer kbType, Integer pageNum,
                                                            Integer pageSize);

    PageResult<RagKnowledgeBaseVO> pageCollectedKnowledgeBases(String keyword, Integer kbType, Integer pageNum,
                                                               Integer pageSize);

    RagKnowledgeBaseVO getMyKnowledgeBase(Long kbId);

    boolean isKnowledgeBaseCollected(Long kbId);

    void collectKnowledgeBase(Long kbId);

    void cancelKnowledgeBaseCollection(Long kbId);

    PageResult<RagDocumentVO> pageKnowledgeBaseDocuments(Long kbId, Integer pageNum, Integer pageSize, String docType,
                                                          String docName);

    List<RagDocumentVO> listPublicKnowledgeBaseDocuments(Long kbId);

    void createKnowledgeBase(String kbName, String description, Integer kbType, Integer isPublic, MultipartFile file);

    void updateKnowledgeBase(Long kbId, String kbName, String description, Integer kbType, Integer isPublic,
                             Integer status, MultipartFile file);

    void updateRagDocument(Long kbId, Long docId, String docName, String description);

    void deleteRagDocument(Long kbId, Long docId);

    void uploadRagFile(HttpServletRequest request, MultipartFile file, String description, Long kbId);

    String chatTest(String message, List<MultipartFile> files);

    float[] embeddingTest(String message);
}
