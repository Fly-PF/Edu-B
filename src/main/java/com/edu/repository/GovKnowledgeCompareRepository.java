package com.edu.repository;

import com.edu.pojo.po.gov.GovKnowledgeComparePO;

import java.util.List;

public interface GovKnowledgeCompareRepository {
    List<GovKnowledgeComparePO> selectVisibleCompareByKnowledgeIds(List<Long> knowledgeIds);

    List<GovKnowledgeComparePO> selectCompareByKnowledgeIds(List<Long> knowledgeIds);

    List<GovKnowledgeComparePO> selectCompareByKnowledgeId(Long knowledgeId);

    GovKnowledgeComparePO selectCompareById(Long compareId);

    int insertCompare(GovKnowledgeComparePO compare);

    int updateCompare(GovKnowledgeComparePO compare);

    int logicalDeleteCompare(Long compareId);
}
