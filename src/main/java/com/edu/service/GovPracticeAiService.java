package com.edu.service;

import com.edu.pojo.dto.gov.GovPracticeAiChatRequest;
import com.edu.pojo.vo.gov.GovPracticeAiStreamVO;
import com.edu.pojo.vo.skill.AiSkillPracticeOptionVO;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface GovPracticeAiService {
    List<AiSkillPracticeOptionVO> listSkills(String keyword);

    Flux<ServerSentEvent<GovPracticeAiStreamVO>> chat(GovPracticeAiChatRequest request);
}
