package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.gov.GovKnowledgeProgressMapper;
import com.edu.pojo.po.gov.GovKnowledgeProgressPO;
import com.edu.repository.GovKnowledgeProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GovKnowledgeProgressRepositoryImpl implements GovKnowledgeProgressRepository {
    private final GovKnowledgeProgressMapper govKnowledgeProgressMapper;

    @Override
    public GovKnowledgeProgressPO selectProgress(Long userId, Long knowledgeId) {
        return govKnowledgeProgressMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeProgressPO>()
                .eq(GovKnowledgeProgressPO::getUserId, userId)
                .eq(GovKnowledgeProgressPO::getKnowledgeId, knowledgeId)
                .eq(GovKnowledgeProgressPO::getDeleted, 0));
    }

    @Override
    public List<GovKnowledgeProgressPO> selectProgressByKnowledgeIds(Long userId, List<Long> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return List.of();
        }
        return govKnowledgeProgressMapper.selectList(new LambdaQueryWrapper<GovKnowledgeProgressPO>()
                .eq(GovKnowledgeProgressPO::getUserId, userId)
                .eq(GovKnowledgeProgressPO::getDeleted, 0)
                .in(GovKnowledgeProgressPO::getKnowledgeId, knowledgeIds));
    }

    @Override
    public int insertProgress(GovKnowledgeProgressPO progress) {
        return govKnowledgeProgressMapper.insert(progress);
    }

    @Override
    public int updateProgress(GovKnowledgeProgressPO progress) {
        return govKnowledgeProgressMapper.update(progress, new LambdaUpdateWrapper<GovKnowledgeProgressPO>()
                .eq(GovKnowledgeProgressPO::getId, progress.getId())
                .eq(GovKnowledgeProgressPO::getDeleted, 0));
    }

    @Override
    public int logicalDeleteProgressByKnowledgeIds(List<Long> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return 0;
        }
        return govKnowledgeProgressMapper.update(null, new LambdaUpdateWrapper<GovKnowledgeProgressPO>()
                .in(GovKnowledgeProgressPO::getKnowledgeId, knowledgeIds)
                .eq(GovKnowledgeProgressPO::getDeleted, 0)
                .set(GovKnowledgeProgressPO::getDeleted, 1));
    }
}
