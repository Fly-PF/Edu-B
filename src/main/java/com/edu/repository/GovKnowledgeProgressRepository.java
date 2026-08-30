package com.edu.repository;

import com.edu.pojo.po.gov.GovKnowledgeProgressPO;

import java.util.List;

public interface GovKnowledgeProgressRepository {
    GovKnowledgeProgressPO selectProgress(Long userId, Long knowledgeId);

    List<GovKnowledgeProgressPO> selectProgressByKnowledgeIds(Long userId, List<Long> knowledgeIds);

    int insertProgress(GovKnowledgeProgressPO progress);

    int updateProgress(GovKnowledgeProgressPO progress);

    int logicalDeleteProgressByKnowledgeIds(List<Long> knowledgeIds);
}
