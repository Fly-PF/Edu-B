package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.gov.GovKnowledgeCompareMapper;
import com.edu.pojo.po.gov.GovKnowledgeComparePO;
import com.edu.repository.GovKnowledgeCompareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GovKnowledgeCompareRepositoryImpl implements GovKnowledgeCompareRepository {
    private final GovKnowledgeCompareMapper govKnowledgeCompareMapper;

    @Override
    public List<GovKnowledgeComparePO> selectVisibleCompareByKnowledgeIds(List<Long> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return List.of();
        }
        return govKnowledgeCompareMapper.selectList(new LambdaQueryWrapper<GovKnowledgeComparePO>()
                .in(GovKnowledgeComparePO::getKnowledgeId, knowledgeIds)
                .eq(GovKnowledgeComparePO::getStatus, 1)
                .eq(GovKnowledgeComparePO::getDeleted, 0)
                .orderByAsc(GovKnowledgeComparePO::getSortOrder)
                .orderByAsc(GovKnowledgeComparePO::getId));
    }

    @Override
    public List<GovKnowledgeComparePO> selectCompareByKnowledgeIds(List<Long> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return List.of();
        }
        return govKnowledgeCompareMapper.selectList(new LambdaQueryWrapper<GovKnowledgeComparePO>()
                .in(GovKnowledgeComparePO::getKnowledgeId, knowledgeIds)
                .eq(GovKnowledgeComparePO::getDeleted, 0)
                .orderByAsc(GovKnowledgeComparePO::getKnowledgeId)
                .orderByAsc(GovKnowledgeComparePO::getSortOrder)
                .orderByAsc(GovKnowledgeComparePO::getId));
    }

    @Override
    public List<GovKnowledgeComparePO> selectCompareByKnowledgeId(Long knowledgeId) {
        if (knowledgeId == null || knowledgeId <= 0) {
            return List.of();
        }
        return govKnowledgeCompareMapper.selectList(new LambdaQueryWrapper<GovKnowledgeComparePO>()
                .eq(GovKnowledgeComparePO::getKnowledgeId, knowledgeId)
                .eq(GovKnowledgeComparePO::getDeleted, 0)
                .orderByAsc(GovKnowledgeComparePO::getSortOrder)
                .orderByAsc(GovKnowledgeComparePO::getId));
    }

    @Override
    public GovKnowledgeComparePO selectCompareById(Long compareId) {
        if (compareId == null || compareId <= 0) {
            return null;
        }
        return govKnowledgeCompareMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeComparePO>()
                .eq(GovKnowledgeComparePO::getId, compareId)
                .eq(GovKnowledgeComparePO::getDeleted, 0));
    }

    @Override
    public int insertCompare(GovKnowledgeComparePO compare) {
        return govKnowledgeCompareMapper.insert(compare);
    }

    @Override
    public int updateCompare(GovKnowledgeComparePO compare) {
        return govKnowledgeCompareMapper.update(compare, new LambdaUpdateWrapper<GovKnowledgeComparePO>()
                .eq(GovKnowledgeComparePO::getId, compare.getId()));
    }

    @Override
    public int logicalDeleteCompare(Long compareId) {
        return govKnowledgeCompareMapper.update(null, new LambdaUpdateWrapper<GovKnowledgeComparePO>()
                .eq(GovKnowledgeComparePO::getId, compareId)
                .eq(GovKnowledgeComparePO::getDeleted, 0)
                .set(GovKnowledgeComparePO::getDeleted, 1)
                .set(GovKnowledgeComparePO::getUpdateTime, LocalDateTime.now()));
    }
}
