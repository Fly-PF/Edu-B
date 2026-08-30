package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.gov.GovKnowledgeNoteMapper;
import com.edu.pojo.po.gov.GovKnowledgeNotePO;
import com.edu.repository.GovKnowledgeNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GovKnowledgeNoteRepositoryImpl implements GovKnowledgeNoteRepository {
    private final GovKnowledgeNoteMapper govKnowledgeNoteMapper;

    @Override
    public GovKnowledgeNotePO selectNote(Long userId, Long knowledgeId) {
        return govKnowledgeNoteMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeNotePO>()
                .eq(GovKnowledgeNotePO::getUserId, userId)
                .eq(GovKnowledgeNotePO::getKnowledgeId, knowledgeId)
                .eq(GovKnowledgeNotePO::getDeleted, 0));
    }

    @Override
    public GovKnowledgeNotePO selectRecord(Long userId, Long knowledgeId) {
        return govKnowledgeNoteMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeNotePO>()
                .eq(GovKnowledgeNotePO::getUserId, userId)
                .eq(GovKnowledgeNotePO::getKnowledgeId, knowledgeId));
    }

    @Override
    public List<GovKnowledgeNotePO> selectNotesByUser(Long userId) {
        return govKnowledgeNoteMapper.selectList(new LambdaQueryWrapper<GovKnowledgeNotePO>()
                .eq(GovKnowledgeNotePO::getUserId, userId)
                .eq(GovKnowledgeNotePO::getDeleted, 0)
                .orderByDesc(GovKnowledgeNotePO::getUpdateTime)
                .orderByDesc(GovKnowledgeNotePO::getId));
    }

    @Override
    public int insertNote(GovKnowledgeNotePO note) {
        return govKnowledgeNoteMapper.insert(note);
    }

    @Override
    public int updateNote(GovKnowledgeNotePO note) {
        return govKnowledgeNoteMapper.update(note, new LambdaUpdateWrapper<GovKnowledgeNotePO>()
                .eq(GovKnowledgeNotePO::getId, note.getId()));
    }

    @Override
    public int logicalDeleteNote(Long userId, Long knowledgeId) {
        return govKnowledgeNoteMapper.update(null, new LambdaUpdateWrapper<GovKnowledgeNotePO>()
                .eq(GovKnowledgeNotePO::getUserId, userId)
                .eq(GovKnowledgeNotePO::getKnowledgeId, knowledgeId)
                .eq(GovKnowledgeNotePO::getDeleted, 0)
                .set(GovKnowledgeNotePO::getDeleted, 1)
                .set(GovKnowledgeNotePO::getUpdateTime, LocalDateTime.now()));
    }
}
