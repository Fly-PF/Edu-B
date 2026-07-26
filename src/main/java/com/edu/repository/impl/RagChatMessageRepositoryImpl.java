package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.RagChatMessageMapper;
import com.edu.pojo.po.RagChatMessagePO;
import com.edu.repository.RagChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RagChatMessageRepositoryImpl implements RagChatMessageRepository {
    private final RagChatMessageMapper ragChatMessageMapper;

    @Override
    public int insertMessage(RagChatMessagePO message) {
        return ragChatMessageMapper.insert(message);
    }

    @Override
    public int updateMessage(RagChatMessagePO message) {
        return ragChatMessageMapper.updateById(message);
    }

    @Override
    public List<RagChatMessagePO> selectSessionMessages(Long sessionId) {
        return ragChatMessageMapper.selectList(new LambdaQueryWrapper<RagChatMessagePO>()
                .eq(RagChatMessagePO::getSessionId, sessionId)
                .eq(RagChatMessagePO::getDeleted, 0)
                .orderByAsc(RagChatMessagePO::getCreateTime)
                .orderByAsc(RagChatMessagePO::getId));
    }

    @Override
    public List<RagChatMessagePO> selectLatestSessionMessages(Long sessionId, Integer limit) {
        return ragChatMessageMapper.selectList(new LambdaQueryWrapper<RagChatMessagePO>()
                .eq(RagChatMessagePO::getSessionId, sessionId)
                .eq(RagChatMessagePO::getDeleted, 0)
                .orderByDesc(RagChatMessagePO::getCreateTime)
                .orderByDesc(RagChatMessagePO::getId)
                .last("limit " + limit));
    }

    @Override
    public List<Long> selectSessionMessageIds(Long sessionId) {
        return ragChatMessageMapper.selectList(new LambdaQueryWrapper<RagChatMessagePO>()
                        .select(RagChatMessagePO::getId)
                        .eq(RagChatMessagePO::getSessionId, sessionId)
                        .eq(RagChatMessagePO::getDeleted, 0))
                .stream()
                .map(RagChatMessagePO::getId)
                .toList();
    }

    @Override
    public int logicalDeleteSessionMessages(Long sessionId) {
        return ragChatMessageMapper.update(new LambdaUpdateWrapper<RagChatMessagePO>()
                .eq(RagChatMessagePO::getSessionId, sessionId)
                .eq(RagChatMessagePO::getDeleted, 0)
                .set(RagChatMessagePO::getDeleted, 1));
    }

    @Override
    public int logicalDeleteMessagesByIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }
        return ragChatMessageMapper.update(new LambdaUpdateWrapper<RagChatMessagePO>()
                .in(RagChatMessagePO::getId, messageIds)
                .eq(RagChatMessagePO::getDeleted, 0)
                .set(RagChatMessagePO::getDeleted, 1));
    }

    @Override
    public boolean existsActiveQaImage(String objectName) {
        return ragChatMessageMapper.existsActiveQaImage(objectName);
    }

}
