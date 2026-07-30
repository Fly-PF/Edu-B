package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.mapper.RagChatSessionMapper;
import com.edu.mapper.RagSessionKbRefMapper;
import com.edu.pojo.po.RagChatSessionPO;
import com.edu.pojo.po.RagSessionKbRefPO;
import com.edu.repository.RagChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class RagChatSessionRepositoryImpl implements RagChatSessionRepository {
    private final RagChatSessionMapper ragChatSessionMapper;
    private final RagSessionKbRefMapper ragSessionKbRefMapper;

    @Override
    public int insertChatSession(RagChatSessionPO chatSession) {
        return ragChatSessionMapper.insert(chatSession);
    }

    @Override
    public RagChatSessionPO selectUserChatSession(Long sessionId, Long userId) {
        return ragChatSessionMapper.selectOne(new LambdaQueryWrapper<RagChatSessionPO>()
                .eq(RagChatSessionPO::getId, sessionId)
                .eq(RagChatSessionPO::getUserId, userId)
                .eq(RagChatSessionPO::getDeleted, 0));
    }

    @Override
    public IPage<RagChatSessionPO> selectUserChatSessionPage(long pageNum, long pageSize, Long userId) {
        LambdaQueryWrapper<RagChatSessionPO> queryWrapper = new LambdaQueryWrapper<RagChatSessionPO>()
                .eq(RagChatSessionPO::getUserId, userId)
                .eq(RagChatSessionPO::getDeleted, 0)
                .orderByDesc(RagChatSessionPO::getCreateTime)
                .orderByDesc(RagChatSessionPO::getId);
        return ragChatSessionMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
    }

    @Override
    public int renameUserChatSession(Long sessionId, Long userId, String sessionName) {
        return ragChatSessionMapper.update(new LambdaUpdateWrapper<RagChatSessionPO>()
                .eq(RagChatSessionPO::getId, sessionId)
                .eq(RagChatSessionPO::getUserId, userId)
                .eq(RagChatSessionPO::getDeleted, 0)
                .set(RagChatSessionPO::getSessionName, sessionName)
                .set(RagChatSessionPO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public int logicalDeleteUserChatSession(Long sessionId, Long userId) {
        return ragChatSessionMapper.update(new LambdaUpdateWrapper<RagChatSessionPO>()
                .eq(RagChatSessionPO::getId, sessionId)
                .eq(RagChatSessionPO::getUserId, userId)
                .eq(RagChatSessionPO::getDeleted, 0)
                .set(RagChatSessionPO::getDeleted, 1)
                .set(RagChatSessionPO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public int logicalDeleteSessionKbRefs(Long sessionId) {
        return ragSessionKbRefMapper.update(new LambdaUpdateWrapper<RagSessionKbRefPO>()
                .eq(RagSessionKbRefPO::getSessionId, sessionId)
                .eq(RagSessionKbRefPO::getDeleted, 0)
                .set(RagSessionKbRefPO::getDeleted, 1));
    }

    @Override
    public int insertSessionKbRef(RagSessionKbRefPO sessionKbRef) {
        return ragSessionKbRefMapper.insert(sessionKbRef);
    }
}
