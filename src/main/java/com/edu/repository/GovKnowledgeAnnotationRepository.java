package com.edu.repository;

import com.edu.pojo.po.gov.GovKnowledgeAnnotationPO;

import java.util.List;

public interface GovKnowledgeAnnotationRepository {
    GovKnowledgeAnnotationPO selectAnnotation(Long userId, Long annotationId);

    GovKnowledgeAnnotationPO selectRecord(Long userId, Long knowledgeId, Long annotationId);

    List<GovKnowledgeAnnotationPO> selectAnnotationsByUser(Long userId);

    List<GovKnowledgeAnnotationPO> selectAnnotationsByUserAndKnowledge(Long userId, Long knowledgeId);

    int insertAnnotation(GovKnowledgeAnnotationPO annotation);

    int updateAnnotation(GovKnowledgeAnnotationPO annotation);

    int logicalDeleteAnnotation(Long userId, Long annotationId);
}
