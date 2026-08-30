package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.gov.GovKnowledgeAnnotationMapper;
import com.edu.pojo.po.gov.GovKnowledgeAnnotationPO;
import com.edu.repository.GovKnowledgeAnnotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GovKnowledgeAnnotationRepositoryImpl implements GovKnowledgeAnnotationRepository {
    private final GovKnowledgeAnnotationMapper govKnowledgeAnnotationMapper;

    @Override
    public GovKnowledgeAnnotationPO selectAnnotation(Long userId, Long annotationId) {
        return govKnowledgeAnnotationMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeAnnotationPO>()
                .eq(GovKnowledgeAnnotationPO::getUserId, userId)
                .eq(GovKnowledgeAnnotationPO::getId, annotationId)
                .eq(GovKnowledgeAnnotationPO::getDeleted, 0));
    }

    @Override
    public GovKnowledgeAnnotationPO selectRecord(Long userId, Long knowledgeId, Long annotationId) {
        return govKnowledgeAnnotationMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeAnnotationPO>()
                .eq(GovKnowledgeAnnotationPO::getUserId, userId)
                .eq(GovKnowledgeAnnotationPO::getKnowledgeId, knowledgeId)
                .eq(GovKnowledgeAnnotationPO::getId, annotationId));
    }

    @Override
    public List<GovKnowledgeAnnotationPO> selectAnnotationsByUser(Long userId) {
        return govKnowledgeAnnotationMapper.selectList(new LambdaQueryWrapper<GovKnowledgeAnnotationPO>()
                .eq(GovKnowledgeAnnotationPO::getUserId, userId)
                .eq(GovKnowledgeAnnotationPO::getDeleted, 0)
                .orderByDesc(GovKnowledgeAnnotationPO::getUpdateTime)
                .orderByDesc(GovKnowledgeAnnotationPO::getId));
    }

    @Override
    public List<GovKnowledgeAnnotationPO> selectAnnotationsByUserAndKnowledge(Long userId, Long knowledgeId) {
        return govKnowledgeAnnotationMapper.selectList(new LambdaQueryWrapper<GovKnowledgeAnnotationPO>()
                .eq(GovKnowledgeAnnotationPO::getUserId, userId)
                .eq(GovKnowledgeAnnotationPO::getKnowledgeId, knowledgeId)
                .eq(GovKnowledgeAnnotationPO::getDeleted, 0)
                .orderByAsc(GovKnowledgeAnnotationPO::getStartOffset)
                .orderByAsc(GovKnowledgeAnnotationPO::getId));
    }

    @Override
    public int insertAnnotation(GovKnowledgeAnnotationPO annotation) {
        return govKnowledgeAnnotationMapper.insert(annotation);
    }

    @Override
    public int updateAnnotation(GovKnowledgeAnnotationPO annotation) {
        return govKnowledgeAnnotationMapper.update(annotation, new LambdaUpdateWrapper<GovKnowledgeAnnotationPO>()
                .eq(GovKnowledgeAnnotationPO::getId, annotation.getId()));
    }

    @Override
    public int logicalDeleteAnnotation(Long userId, Long annotationId) {
        return govKnowledgeAnnotationMapper.update(null, new LambdaUpdateWrapper<GovKnowledgeAnnotationPO>()
                .eq(GovKnowledgeAnnotationPO::getUserId, userId)
                .eq(GovKnowledgeAnnotationPO::getId, annotationId)
                .eq(GovKnowledgeAnnotationPO::getDeleted, 0)
                .set(GovKnowledgeAnnotationPO::getDeleted, 1)
                .set(GovKnowledgeAnnotationPO::getUpdateTime, LocalDateTime.now()));
    }
}
