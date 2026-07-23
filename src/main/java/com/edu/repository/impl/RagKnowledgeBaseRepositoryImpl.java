package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.RagKnowledgeBaseMapper;
import com.edu.pojo.po.RagKnowledgeBasePO;
import com.edu.repository.RagKnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RagKnowledgeBaseRepositoryImpl implements RagKnowledgeBaseRepository {
    private final RagKnowledgeBaseMapper ragKnowledgeBaseMapper;

    @Override
    public int insertKnowledgeBase(RagKnowledgeBasePO knowledgeBase) {
        return ragKnowledgeBaseMapper.insert(knowledgeBase);
    }

    @Override
    public RagKnowledgeBasePO selectKnowledgeBaseById(Long id, Long userId) {
        return ragKnowledgeBaseMapper.selectOne(new LambdaQueryWrapper<RagKnowledgeBasePO>()
                .eq(RagKnowledgeBasePO::getId, id)
                .eq(RagKnowledgeBasePO::getUserId, userId)
                .eq(RagKnowledgeBasePO::getDeleted, 0));
    }

    @Override
    public RagKnowledgeBasePO selectKnowledgeBaseById(Long id) {
        return ragKnowledgeBaseMapper.selectOne(new LambdaQueryWrapper<RagKnowledgeBasePO>()
                .eq(RagKnowledgeBasePO::getId, id));
    }

    @Override
    public int updateKnowledgeBase(RagKnowledgeBasePO knowledgeBase) {
        return ragKnowledgeBaseMapper.updateById(knowledgeBase);
    }

    @Override
    public List<RagKnowledgeBasePO> selectUserKnowledgeBases(Long userId, String keyword, Integer status, Integer isPublic,
                                                            Integer kbType) {
        return ragKnowledgeBaseMapper.selectList(new LambdaQueryWrapper<RagKnowledgeBasePO>()
                .eq(RagKnowledgeBasePO::getUserId, userId)
                .eq(RagKnowledgeBasePO::getDeleted, 0)
                .eq(status != null, RagKnowledgeBasePO::getStatus, status)
                .eq(isPublic != null, RagKnowledgeBasePO::getPublicFlag, isPublic)
                .eq(kbType != null, RagKnowledgeBasePO::getKbType, kbType)
                .like(StringUtils.hasText(keyword), RagKnowledgeBasePO::getKbName, keyword == null ? null : keyword.trim())
                .orderByDesc(RagKnowledgeBasePO::getUpdateTime)
                .orderByDesc(RagKnowledgeBasePO::getId));
    }
}
