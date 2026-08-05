package com.edu.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.pojo.po.RagKnowledgeBasePO;

import java.util.List;

public interface RagKnowledgeBaseRepository {
    int insertKnowledgeBase(RagKnowledgeBasePO knowledgeBase);

    RagKnowledgeBasePO selectKnowledgeBaseById(Long id, Long userId);

    RagKnowledgeBasePO selectLegacyKnowledgeBaseById(Long id, Long userId);

    RagKnowledgeBasePO selectKnowledgeBaseById(Long id);

    RagKnowledgeBasePO selectPublicKnowledgeBaseById(Long id);

    RagKnowledgeBasePO selectSelectableKnowledgeBase(Long userId, Long id);

    int updateKnowledgeBase(RagKnowledgeBasePO knowledgeBase);

    int logicalDeleteKnowledgeBase(Long kbId, Long userId);

    List<RagKnowledgeBasePO> selectUserKnowledgeBases(Long userId, String keyword, Integer status, Integer isPublic,
                                                      Integer kbType);

    List<RagKnowledgeBasePO> selectPublicKnowledgeBases(Integer kbType, Integer limit);

    List<RagKnowledgeBasePO> selectSessionKnowledgeBases(Long sessionId);

    IPage<RagKnowledgeBasePO> selectPublicKnowledgeBasePage(long pageNum, long pageSize, String keyword, Integer kbType);

    IPage<RagKnowledgeBasePO> selectCollectedKnowledgeBasePage(long pageNum, long pageSize, Long userId, String keyword,
                                                              Integer kbType);
}
