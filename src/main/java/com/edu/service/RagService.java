package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.dto.rag.RagChatRequest;
import com.edu.pojo.dto.rag.RagChatSessionCreateRequest;
import com.edu.pojo.dto.rag.RagChatSessionRenameRequest;
import com.edu.pojo.vo.ai.AiCompanionMaterialExcerpt;
import com.edu.pojo.vo.rag.RagChatMessageVO;
import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import com.edu.pojo.vo.rag.RagChatSessionVO;
import com.edu.pojo.vo.rag.RagDocumentVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

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

    PageResult<RagChatSessionVO> pageChatSessions(Integer pageNum, Integer pageSize);

    List<RagKnowledgeBaseVO> listChatSessionKnowledgeBases(Long sessionId);

    RagChatSessionVO createChatSession(RagChatSessionCreateRequest request);

    RagChatSessionVO renameChatSession(RagChatSessionRenameRequest request);

    void deleteChatSession(Long sessionId);

    List<RagChatMessageVO> listChatMessages(Long sessionId);

    void deleteChatMessagePair(Long sessionId, String messageId);

    Flux<ServerSentEvent<RagChatMessageVO>> chat(RagChatRequest request);

    String prepareSpeechText(String content);

    boolean existsChatImage(String objectName);

    PageResult<RagDocumentVO> pageKnowledgeBaseDocuments(Long kbId, Integer pageNum, Integer pageSize, String docType,
                                                          String docName);

    List<RagDocumentVO> listPublicKnowledgeBaseDocuments(Long kbId);

    void createKnowledgeBase(String kbName, String description, Integer kbType, Integer isPublic, MultipartFile file);

    void updateKnowledgeBase(Long kbId, String kbName, String description, Integer kbType, Integer isPublic,
                             Integer status, MultipartFile file);

    void deleteKnowledgeBase(Long kbId);

    void updateKnowledgeBaseDocument(Long kbId, Long docId, String docName, String description);

    void deleteKnowledgeBaseDocument(Long kbId, Long docId);

    void uploadKnowledgeBaseDocument(HttpServletRequest request, MultipartFile file, String description, Long kbId);

    void ensureCourseKnowledgeBase(Long courseId, Long ownerId, String courseName, String description);

    void uploadCourseResourceDocument(Long courseId, Long resourceId, MultipartFile file, String description);

    void deleteCourseResourceDocument(Long courseId, Long resourceId);

    void deleteCourseKnowledgeBase(Long courseId);

    List<AiCompanionMaterialExcerpt> retrieveCourseMaterials(Long courseId, String question);
}
