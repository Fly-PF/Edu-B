package com.edu.service;

import com.edu.pojo.vo.ai.AiCompanionWebSource;

import java.util.List;

public interface AiCompanionWebSearchService {
    List<AiCompanionWebSource> search(String question);
}
