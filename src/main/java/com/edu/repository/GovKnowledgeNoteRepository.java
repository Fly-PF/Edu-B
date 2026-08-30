package com.edu.repository;

import com.edu.pojo.po.gov.GovKnowledgeNotePO;

import java.util.List;

public interface GovKnowledgeNoteRepository {
    GovKnowledgeNotePO selectNote(Long userId, Long knowledgeId);

    GovKnowledgeNotePO selectRecord(Long userId, Long knowledgeId);

    List<GovKnowledgeNotePO> selectNotesByUser(Long userId);

    int insertNote(GovKnowledgeNotePO note);

    int updateNote(GovKnowledgeNotePO note);

    int logicalDeleteNote(Long userId, Long knowledgeId);
}
