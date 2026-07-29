package com.edu.service;

import com.edu.pojo.dto.ai.AiCompanionExchangeRequest;
import com.edu.pojo.dto.ai.AiCompanionSessionCreateRequest;
import com.edu.pojo.vo.ai.AiCompanionContextVO;
import com.edu.pojo.vo.ai.AiCompanionMessageVO;
import com.edu.pojo.vo.ai.AiCompanionSessionVO;

import java.util.List;

public interface AiCompanionService {
    AiCompanionContextVO getContext(Long courseId, Long chapterId, Long resourceId);

    AiCompanionSessionVO createSession(AiCompanionSessionCreateRequest request);

    List<AiCompanionSessionVO> listSessions(Long courseId);

    List<AiCompanionMessageVO> appendExchange(Long sessionId, AiCompanionExchangeRequest request);

    List<AiCompanionMessageVO> listMessages(Long sessionId);

    void clearConversations();
}
