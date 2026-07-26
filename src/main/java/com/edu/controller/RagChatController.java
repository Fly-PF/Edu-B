package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.rag.RagChatRequest;
import com.edu.pojo.dto.rag.RagChatSessionCreateRequest;
import com.edu.pojo.dto.rag.RagChatSessionRenameRequest;
import com.edu.pojo.vo.rag.RagChatMessageVO;
import com.edu.pojo.vo.rag.RagChatSessionVO;
import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import com.edu.service.RagFileAccessService;
import com.edu.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
@Validated
@Tag(name = "RAG")
public class RagChatController {
    private final RagService ragService;
    private final RagFileAccessService ragFileAccessService;

    @Operation(summary = "分页查询RAG聊天会话")
    @GetMapping("/chat/session/page")
    public Result<PageResult<RagChatSessionVO>> pageChatSessions(@RequestParam(required = false) Integer pageNum,
                                                                 @RequestParam(required = false) Integer pageSize) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.pageChatSessions(pageNum, pageSize));
    }

    @Operation(summary = "查询RAG聊天会话知识库")
    @GetMapping("/chat/session/kb")
    public Result<List<RagKnowledgeBaseVO>> listChatSessionKnowledgeBases(@RequestParam("session_id") @NotNull @Min(1) Long sessionId) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.listChatSessionKnowledgeBases(sessionId));
    }

    @Operation(summary = "创建RAG聊天会话")
    @PostMapping("/chat/session")
    public Result<RagChatSessionVO> createChatSession(@Valid @RequestBody RagChatSessionCreateRequest request) {
        return Result.setResult(HttpStatus.CREATED, "创建成功", ragService.createChatSession(request));
    }

    @Operation(summary = "重命名RAG聊天会话")
    @PostMapping("/chat/session/rename")
    public Result<RagChatSessionVO> renameChatSession(@Valid @RequestBody RagChatSessionRenameRequest request) {
        return Result.setResult(HttpStatus.OK, "重命名成功", ragService.renameChatSession(request));
    }

    @Operation(summary = "删除RAG聊天会话")
    @PostMapping("/chat/session/delete")
    public Result<Void> deleteChatSession(@RequestParam("session_id") @NotNull @Min(1) Long sessionId) {
        ragService.deleteChatSession(sessionId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }

    @Operation(summary = "查询RAG聊天历史消息")
    @GetMapping("/chat/message")
    public Result<List<RagChatMessageVO>> listChatMessages(@RequestParam("session_id") @NotNull @Min(1) Long sessionId) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.listChatMessages(sessionId));
    }

    @Operation(summary = "RAG流式聊天")
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public Flux<ServerSentEvent<RagChatMessageVO>> chat(@Valid @ModelAttribute RagChatRequest request) {
        return ragService.chat(request);
    }

    @Operation(summary = "获取聊天图片")
    @GetMapping("/chat/image")
    public ResponseEntity<byte[]> getChatImage(@RequestParam String objectName) {
        return ragFileAccessService.getChatImage(objectName);
    }
}
