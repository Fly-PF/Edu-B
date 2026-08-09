package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.ai.AiCompanionExchangeRequest;
import com.edu.pojo.dto.ai.AiCompanionSessionCreateRequest;
import com.edu.pojo.vo.ai.AiCompanionContextVO;
import com.edu.pojo.vo.ai.AiCompanionMessageVO;
import com.edu.pojo.vo.ai.AiCompanionSessionVO;
import com.edu.service.AiCompanionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/ai-companion")
@Tag(name = "学生智能学伴")
public class StudentAiCompanionController {
    private final AiCompanionService aiCompanionService;

    @Operation(summary = "创建智能学伴会话")
    @PostMapping("/sessions")
    public Result<AiCompanionSessionVO> createSession(@RequestBody AiCompanionSessionCreateRequest request) {
        return Result.setResult(HttpStatus.CREATED, "创建成功", aiCompanionService.createSession(request));
    }

    @Operation(summary = "查询当前学生的智能学伴会话")
    @GetMapping("/sessions")
    public Result<List<AiCompanionSessionVO>> listSessions(
            @RequestParam(required = false) Long courseId
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiCompanionService.listSessions(courseId));
    }

    @Operation(summary = "保存一轮智能学伴问答")
    @PostMapping("/sessions/{sessionId}/messages")
    public Result<List<AiCompanionMessageVO>> appendExchange(
            @PathVariable Long sessionId,
            @RequestBody AiCompanionExchangeRequest request
    ) {
        return Result.setResult(HttpStatus.CREATED, "保存成功", aiCompanionService.appendExchange(sessionId, request));
    }

    @Operation(summary = "查询智能学伴会话消息")
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<AiCompanionMessageVO>> listMessages(@PathVariable Long sessionId) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiCompanionService.listMessages(sessionId));
    }

    @Operation(summary = "删除当前学生的指定智能学伴对话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteConversation(@PathVariable Long sessionId) {
        aiCompanionService.deleteConversation(sessionId);
        return Result.setResult(HttpStatus.OK, "当前对话已删除", null);
    }

    @Operation(summary = "清空当前学生的智能学伴对话")
    @DeleteMapping("/sessions")
    public Result<Void> clearConversations() {
        aiCompanionService.clearConversations();
        return Result.setResult(HttpStatus.OK, "已清空全部对话", null);
    }

    @Operation(summary = "获取当前课程学习上下文")
    @GetMapping("/context")
    public Result<AiCompanionContextVO> getContext(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) Long resourceId
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "查询成功",
                aiCompanionService.getContext(courseId, chapterId, resourceId)
        );
    }
}
