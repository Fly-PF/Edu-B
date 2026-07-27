package com.edu.service;

import com.edu.pojo.dto.ai.AiDrawGuessRequest;
import com.edu.pojo.vo.ai.AiDrawGuessResultVO;

public interface AiDrawGuessService {
    AiDrawGuessResultVO guess(AiDrawGuessRequest request);
}
