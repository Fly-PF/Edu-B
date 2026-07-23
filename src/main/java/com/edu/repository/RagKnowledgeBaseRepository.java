package com.edu.repository;

import com.edu.pojo.po.RagKnowledgeBasePO;

import java.util.List;

public interface RagKnowledgeBaseRepository {
    int insertKnowledgeBase(RagKnowledgeBasePO knowledgeBase);

    RagKnowledgeBasePO selectKnowledgeBaseById(Long id, Long userId);

    RagKnowledgeBasePO selectKnowledgeBaseById(Long id);

    int updateKnowledgeBase(RagKnowledgeBasePO knowledgeBase);

    List<RagKnowledgeBasePO> selectUserKnowledgeBases(Long userId, String keyword, Integer status, Integer isPublic,
                                                      Integer kbType);
}
