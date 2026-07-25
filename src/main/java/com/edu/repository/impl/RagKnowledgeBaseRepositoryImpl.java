package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public RagKnowledgeBasePO selectPublicKnowledgeBaseById(Long id) {
        return ragKnowledgeBaseMapper.selectOne(new LambdaQueryWrapper<RagKnowledgeBasePO>()
                .eq(RagKnowledgeBasePO::getId, id)
                .eq(RagKnowledgeBasePO::getPublicFlag, 1)
                .eq(RagKnowledgeBasePO::getStatus, 1)
                .eq(RagKnowledgeBasePO::getDeleted, 0));
    }

    @Override
    public RagKnowledgeBasePO selectSelectableKnowledgeBase(Long userId, Long id) {
        return ragKnowledgeBaseMapper.selectOne(new LambdaQueryWrapper<RagKnowledgeBasePO>()
                .eq(RagKnowledgeBasePO::getId, id)
                .eq(RagKnowledgeBasePO::getStatus, 1)
                .eq(RagKnowledgeBasePO::getDeleted, 0)
                .and(wrapper -> wrapper
                        .eq(RagKnowledgeBasePO::getUserId, userId)
                        .or(publicWrapper -> publicWrapper
                                .eq(RagKnowledgeBasePO::getPublicFlag, 1)
                                .inSql(RagKnowledgeBasePO::getId,
                                        "select kb_id from rag_kb_user_collection where user_id = " + userId + " and deleted = 0"))));
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

    @Override
    public List<RagKnowledgeBasePO> selectPublicKnowledgeBases(Integer kbType, Integer limit) {
        return ragKnowledgeBaseMapper.selectList(new LambdaQueryWrapper<RagKnowledgeBasePO>()
                .eq(RagKnowledgeBasePO::getKbType, kbType)
                .eq(RagKnowledgeBasePO::getPublicFlag, 1)
                .eq(RagKnowledgeBasePO::getStatus, 1)
                .eq(RagKnowledgeBasePO::getDeleted, 0)
                .orderByDesc(RagKnowledgeBasePO::getCreateTime)
                .orderByDesc(RagKnowledgeBasePO::getId)
                .last("LIMIT " + limit));
    }

    @Override
    public List<RagKnowledgeBasePO> selectSessionKnowledgeBases(Long sessionId) {
        return ragKnowledgeBaseMapper.selectList(new LambdaQueryWrapper<RagKnowledgeBasePO>()
                .eq(RagKnowledgeBasePO::getDeleted, 0)
                .inSql(RagKnowledgeBasePO::getId,
                        "select kb_id from rag_session_kb_ref where session_id = " + sessionId + " and deleted = 0")
                .orderByDesc(RagKnowledgeBasePO::getCreateTime)
                .orderByDesc(RagKnowledgeBasePO::getId));
    }

    @Override
    public IPage<RagKnowledgeBasePO> selectPublicKnowledgeBasePage(long pageNum, long pageSize, String keyword,
                                                                  Integer kbType) {
        LambdaQueryWrapper<RagKnowledgeBasePO> queryWrapper = new LambdaQueryWrapper<RagKnowledgeBasePO>()
                .eq(RagKnowledgeBasePO::getPublicFlag, 1)
                .eq(RagKnowledgeBasePO::getStatus, 1)
                .eq(RagKnowledgeBasePO::getDeleted, 0)
                .eq(kbType != null, RagKnowledgeBasePO::getKbType, kbType)
                .like(StringUtils.hasText(keyword), RagKnowledgeBasePO::getKbName, keyword == null ? null : keyword.trim())
                .orderByDesc(RagKnowledgeBasePO::getCreateTime)
                .orderByDesc(RagKnowledgeBasePO::getId);
        return ragKnowledgeBaseMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
    }

    @Override
    public IPage<RagKnowledgeBasePO> selectCollectedKnowledgeBasePage(long pageNum, long pageSize, Long userId,
                                                                     String keyword, Integer kbType) {
        LambdaQueryWrapper<RagKnowledgeBasePO> queryWrapper = new LambdaQueryWrapper<RagKnowledgeBasePO>()
                .eq(RagKnowledgeBasePO::getPublicFlag, 1)
                .eq(RagKnowledgeBasePO::getStatus, 1)
                .eq(RagKnowledgeBasePO::getDeleted, 0)
                .ne(RagKnowledgeBasePO::getUserId, userId)
                .eq(kbType != null, RagKnowledgeBasePO::getKbType, kbType)
                .like(StringUtils.hasText(keyword), RagKnowledgeBasePO::getKbName, keyword == null ? null : keyword.trim())
                .inSql(RagKnowledgeBasePO::getId,
                        "select kb_id from rag_kb_user_collection where user_id = " + userId + " and deleted = 0")
                .orderByDesc(RagKnowledgeBasePO::getCreateTime)
                .orderByDesc(RagKnowledgeBasePO::getId);
        return ragKnowledgeBaseMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
    }
}
