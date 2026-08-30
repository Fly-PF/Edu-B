package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.gov.GovKnowledgeNodeMapper;
import com.edu.pojo.po.gov.GovKnowledgeNodePO;
import com.edu.repository.GovKnowledgeNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GovKnowledgeNodeRepositoryImpl implements GovKnowledgeNodeRepository {
    private final GovKnowledgeNodeMapper govKnowledgeNodeMapper;

    @Override
    public List<GovKnowledgeNodePO> selectExistingNodesBySubject(String subject) {
        return govKnowledgeNodeMapper.selectList(new LambdaQueryWrapper<GovKnowledgeNodePO>()
                .eq(GovKnowledgeNodePO::getSubject, subject)
                .eq(GovKnowledgeNodePO::getDeleted, 0)
                .orderByAsc(GovKnowledgeNodePO::getParentId)
                .orderByAsc(GovKnowledgeNodePO::getSortOrder)
                .orderByAsc(GovKnowledgeNodePO::getId));
    }

    @Override
    public List<GovKnowledgeNodePO> selectVisibleNodesBySubject(String subject) {
        return govKnowledgeNodeMapper.selectList(new LambdaQueryWrapper<GovKnowledgeNodePO>()
                .eq(GovKnowledgeNodePO::getSubject, subject)
                .eq(GovKnowledgeNodePO::getStatus, 1)
                .eq(GovKnowledgeNodePO::getDeleted, 0)
                .orderByAsc(GovKnowledgeNodePO::getParentId)
                .orderByAsc(GovKnowledgeNodePO::getSortOrder)
                .orderByAsc(GovKnowledgeNodePO::getId));
    }

    @Override
    public List<GovKnowledgeNodePO> selectNodesByIds(List<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return List.of();
        }
        return govKnowledgeNodeMapper.selectList(new LambdaQueryWrapper<GovKnowledgeNodePO>()
                .in(GovKnowledgeNodePO::getId, nodeIds)
                .eq(GovKnowledgeNodePO::getDeleted, 0));
    }

    @Override
    public GovKnowledgeNodePO selectNodeById(Long nodeId) {
        return govKnowledgeNodeMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeNodePO>()
                .eq(GovKnowledgeNodePO::getId, nodeId)
                .eq(GovKnowledgeNodePO::getDeleted, 0));
    }

    @Override
    public GovKnowledgeNodePO selectVisibleNodeById(Long nodeId) {
        return govKnowledgeNodeMapper.selectOne(new LambdaQueryWrapper<GovKnowledgeNodePO>()
                .eq(GovKnowledgeNodePO::getId, nodeId)
                .eq(GovKnowledgeNodePO::getStatus, 1)
                .eq(GovKnowledgeNodePO::getDeleted, 0));
    }

    @Override
    public int insertNode(GovKnowledgeNodePO node) {
        return govKnowledgeNodeMapper.insert(node);
    }

    @Override
    public int updateNode(GovKnowledgeNodePO node) {
        return govKnowledgeNodeMapper.update(node, new LambdaUpdateWrapper<GovKnowledgeNodePO>()
                .eq(GovKnowledgeNodePO::getId, node.getId())
                .eq(GovKnowledgeNodePO::getDeleted, 0));
    }

    @Override
    public int logicalDeleteNodes(List<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return 0;
        }
        return govKnowledgeNodeMapper.update(null, new LambdaUpdateWrapper<GovKnowledgeNodePO>()
                .in(GovKnowledgeNodePO::getId, nodeIds)
                .eq(GovKnowledgeNodePO::getDeleted, 0)
                .set(GovKnowledgeNodePO::getDeleted, 1));
    }
}
