package com.edu.repository;

import com.edu.pojo.po.AiCompanionMessagePO;
import com.edu.pojo.po.AiCompanionSessionPO;

import java.time.LocalDateTime;
import java.util.List;

public interface AiCompanionRepository {
    int insertSession(AiCompanionSessionPO session);

    AiCompanionSessionPO selectSessionById(Long sessionId);

    List<AiCompanionSessionPO> selectSessionsByStudentId(Long studentId, Long courseId);

    int updateSessionActivity(Long sessionId, LocalDateTime lastMessageTime);

    int insertMessage(AiCompanionMessagePO message);

    List<AiCompanionMessagePO> selectMessagesBySessionId(Long sessionId);
}
