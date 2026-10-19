package com.edu.service;

import com.edu.pojo.dto.demo.PsDemoProblemAiAskRequest;
import com.edu.pojo.vo.demo.PsDemoProblemAiStreamVO;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface PsDemoProblemAiService {
    Flux<ServerSentEvent<PsDemoProblemAiStreamVO>> chat(PsDemoProblemAiAskRequest request);
}
