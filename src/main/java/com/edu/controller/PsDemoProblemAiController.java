package com.edu.controller;

import com.edu.pojo.dto.demo.PsDemoProblemAiAskRequest;
import com.edu.pojo.vo.demo.PsDemoProblemAiStreamVO;
import com.edu.service.PsDemoProblemAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ps-demo/problem-ai")
@Tag(name = "PS Demo 问 AI")
public class PsDemoProblemAiController {
    private final PsDemoProblemAiService psDemoProblemAiService;

    @Operation(summary = "PS Demo 题目流式问 AI")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public Flux<ServerSentEvent<PsDemoProblemAiStreamVO>> chat(@Valid @RequestBody PsDemoProblemAiAskRequest request) {
        return psDemoProblemAiService.chat(request);
    }
}
