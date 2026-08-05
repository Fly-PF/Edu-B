package com.edu.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.pojo.po.RagDocumentPO;

import java.util.List;

public interface RagDocumentRepository {
    int insertDocument(RagDocumentPO document);

    int deleteDocumentById(Long documentId);

    RagDocumentPO selectKnowledgeBaseDocumentById(Long kbId, Long documentId);

    RagDocumentPO selectKnowledgeBaseDocument(Long kbId, String fileUrl);

    RagDocumentPO selectCourseResourceDocument(Long kbId, Long resourceId);

    List<RagDocumentPO> selectKnowledgeBaseDocuments(Long kbId);

    List<RagDocumentPO> selectDocumentsByIds(List<Long> documentIds);

    int updateKnowledgeBaseDocument(Long kbId, Long documentId, String docName, String description);

    int logicalDeleteKnowledgeBaseDocument(Long kbId, Long documentId);

    int logicalDeleteKnowledgeBaseDocuments(Long kbId);

    IPage<RagDocumentPO> selectKnowledgeBaseDocumentPage(long pageNum, long pageSize, Long kbId, String docType,
                                                         String docName);
}
