package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.gov.GovPracticeAiChatRequest;
import com.edu.pojo.vo.gov.GovPracticeAiStreamVO;
import com.edu.pojo.vo.skill.AiSkillPracticeOptionVO;
import com.edu.service.GovPracticeAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gov/practice-ai")
@Tag(name = "考公练习AI讲题")
public class GovPracticeAiController {
    private final GovPracticeAiService govPracticeAiService;

    @Operation(summary = "查询练习可用Skill")
    @GetMapping("/skills")
    @PreAuthorize("hasAnyAuthority('STUDENT','TEACHER')")
    public Result<List<AiSkillPracticeOptionVO>> listSkills(@RequestParam(required = false) String keyword) {
        return Result.setResult(HttpStatus.OK, "查询成功", govPracticeAiService.listSkills(keyword));
    }

    @Operation(summary = "考公练习流式讲题")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    @PreAuthorize("hasAuthority('STUDENT')")
    public Flux<ServerSentEvent<GovPracticeAiStreamVO>> chat(@RequestBody GovPracticeAiChatRequest request) {
        return govPracticeAiService.chat(request);
    }
}
