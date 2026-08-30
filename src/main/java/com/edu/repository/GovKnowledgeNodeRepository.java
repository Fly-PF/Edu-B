package com.edu.repository;

import com.edu.pojo.po.gov.GovKnowledgeNodePO;

import java.util.List;

public interface GovKnowledgeNodeRepository {
    List<GovKnowledgeNodePO> selectExistingNodesBySubject(String subject);

    List<GovKnowledgeNodePO> selectVisibleNodesBySubject(String subject);

    List<GovKnowledgeNodePO> selectNodesByIds(List<Long> nodeIds);

    GovKnowledgeNodePO selectNodeById(Long nodeId);

    GovKnowledgeNodePO selectVisibleNodeById(Long nodeId);

    int insertNode(GovKnowledgeNodePO node);

    int updateNode(GovKnowledgeNodePO node);

    int logicalDeleteNodes(List<Long> nodeIds);
}
