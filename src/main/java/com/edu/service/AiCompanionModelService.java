package com.edu.service;

import com.edu.pojo.vo.ai.AiCompanionContextVO;
import com.edu.pojo.vo.ai.AiCompanionMessageVO;
import com.edu.pojo.vo.ai.AiCompanionModelResult;

import java.util.List;

public interface AiCompanionModelService {
    AiCompanionModelResult generateAnswer(
            AiCompanionContextVO context,
            List<AiCompanionMessageVO> history,
            String question
    );
}
