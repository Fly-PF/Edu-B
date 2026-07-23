package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.AiCompanionMessageMapper;
import com.edu.mapper.AiCompanionSessionMapper;
import com.edu.pojo.po.AiCompanionMessagePO;
import com.edu.pojo.po.AiCompanionSessionPO;
import com.edu.repository.AiCompanionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiCompanionRepositoryImpl implements AiCompanionRepository {
    private final AiCompanionSessionMapper sessionMapper;
    private final AiCompanionMessageMapper messageMapper;

    @Override
    public int insertSession(AiCompanionSessionPO session) {
        return sessionMapper.insert(session);
    }

    @Override
    public AiCompanionSessionPO selectSessionById(Long sessionId) {
        return sessionMapper.selectOne(new LambdaQueryWrapper<AiCompanionSessionPO>()
                .eq(AiCompanionSessionPO::getId, sessionId)
                .eq(AiCompanionSessionPO::getDeleted, 0));
    }

    @Override
    public List<AiCompanionSessionPO> selectSessionsByStudentId(Long studentId, Long courseId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<AiCompanionSessionPO>()
                .eq(AiCompanionSessionPO::getStudentId, studentId)
                .eq(courseId != null, AiCompanionSessionPO::getCourseId, courseId)
                .eq(AiCompanionSessionPO::getDeleted, 0)
                .orderByDesc(AiCompanionSessionPO::getLastMessageTime)
                .orderByDesc(AiCompanionSessionPO::getId)
                .last("LIMIT 50"));
    }

    @Override
    public int updateSessionActivity(Long sessionId, LocalDateTime lastMessageTime) {
        return sessionMapper.updateById(AiCompanionSessionPO.builder()
                .id(sessionId)
                .lastMessageTime(lastMessageTime)
                .updateTime(lastMessageTime)
                .build());
    }

    @Override
    public int insertMessage(AiCompanionMessagePO message) {
        return messageMapper.insert(message);
    }

    @Override
    public List<AiCompanionMessagePO> selectMessagesBySessionId(Long sessionId) {
        return messageMapper.selectList(new LambdaQueryWrapper<AiCompanionMessagePO>()
                .eq(AiCompanionMessagePO::getSessionId, sessionId)
                .eq(AiCompanionMessagePO::getDeleted, 0)
                .orderByAsc(AiCompanionMessagePO::getCreateTime)
                .orderByAsc(AiCompanionMessagePO::getId));
    }
}
