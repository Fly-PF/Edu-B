package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.rag.RagChatRequest;
import com.edu.pojo.dto.rag.RagChatSessionCreateRequest;
import com.edu.pojo.dto.rag.RagChatSessionRenameRequest;
import com.edu.pojo.dto.rag.RagSpeechTextRequest;
import com.edu.pojo.dto.safety.SafetyGatewayRequest;
import com.edu.pojo.dto.safety.SafetyGatewayResponse;
import com.edu.pojo.dto.safety.SafetyRecordDTO;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyReviewStatus;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import com.edu.pojo.vo.rag.RagChatMessageVO;
import com.edu.pojo.vo.rag.RagChatSessionVO;
import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import com.edu.service.RagFileAccessService;
import com.edu.service.RagService;
import com.edu.service.safety.SafetyGatewaySupport;
import com.edu.service.safety.SafetyGatewayService;
import com.edu.service.safety.SafetyReviewDispatchService;
import com.edu.util.SecurityUtil;
import com.edu.util.SafetyGradeLevelResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
@Validated
@Tag(name = "RAG")
public class RagChatController {
    private final RagService ragService;
    private final RagFileAccessService ragFileAccessService;
    private final SafetyGatewayService safetyGatewayService;
    private final SafetyGatewaySupport safetyGatewaySupport;
    private final SafetyReviewDispatchService safetyReviewDispatchService;

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

    @Operation(summary = "删除RAG聊天消息对")
    @PostMapping("/chat/message/delete")
    public Result<Void> deleteChatMessagePair(@RequestParam("session_id") @NotNull @Min(1) Long sessionId,
                                              @RequestParam("message_id") @NotBlank String messageId) {
        ragService.deleteChatMessagePair(sessionId, messageId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }

    @Operation(summary = "RAG流式聊天")
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public Flux<ServerSentEvent<RagChatMessageVO>> chat(@Valid @ModelAttribute RagChatRequest request) {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        SafetyGatewayResponse safetyResponse = safetyGatewayService.evaluate(SafetyGatewayRequest.builder()
                .sourceModule(SafetySourceModule.EDUCATION_RAG)
                .scene(resolveScene())
                .userRole(resolveUserRole())
                .gradeLevel(resolveGradeLevel())
                .userId(loginUser == null ? null : loginUser.getUserId())
                .inputText(request.getMessage())
                .recordLog(true)
                .metadata(Map.of(
                        "entryPoint", "rag-chat-controller",
                        "route", "/api/rag/chat",
                        "sessionId", String.valueOf(request.getSessionId())
                ))
                .build());
        if (safetyResponse.getDecision() == SafetyDecision.BLOCK) {
            return Flux.just(ServerSentEvent.builder(RagChatMessageVO.builder()
                    .status("error")
                    .sessionId(request.getSessionId())
                    .messageId("safety-" + UUID.randomUUID())
                    .role("assistant")
                    .content(firstNonBlank(safetyResponse.getReason(), "RAG请求被安全层拦截"))
                    .build()).build());
        }
        if (Boolean.TRUE.equals(safetyResponse.getManualReviewRequired())) {
            Long reviewRecordId = safetyResponse.getRecordId();
            if (reviewRecordId == null) {
                return Flux.just(ServerSentEvent.builder(RagChatMessageVO.builder()
                        .status("error")
                        .sessionId(request.getSessionId())
                        .messageId("safety-" + UUID.randomUUID())
                        .role("assistant")
                        .content("人工审核记录创建失败，请稍后重试")
                        .build()).build());
            }
            Flux<ServerSentEvent<RagChatMessageVO>> pendingFrame = Flux.just(ServerSentEvent.builder(
                    RagChatMessageVO.builder()
                            .status("review_pending")
                            .sessionId(request.getSessionId())
                            .messageId("review-" + UUID.randomUUID())
                            .role("assistant")
                            .content("内容已提交人工审核，请等待管理员处理")
                            .reviewRecordId(reviewRecordId)
                            .build()
            ).build());
            Mono<SafetyRecordDTO> reviewMono = safetyReviewDispatchService.awaitDecision(reviewRecordId)
                    .timeout(Duration.ofMinutes(30));
            Flux<ServerSentEvent<RagChatMessageVO>> resultFrame = reviewMono.flatMapMany(review -> {
                if (review != null && review.getReviewStatus() == SafetyReviewStatus.APPROVED) {
                    return ragService.chatForReviewedRequest(loginUser.getUserId(), request, reviewRecordId);
                }
                return Flux.just(ServerSentEvent.builder(RagChatMessageVO.builder()
                        .status("error")
                        .sessionId(request.getSessionId())
                        .messageId("safety-" + UUID.randomUUID())
                        .role("assistant")
                        .content("人工审核未通过，当前问答已停止")
                        .reviewRecordId(reviewRecordId)
                        .build()).build());
            }).onErrorResume(ex -> Flux.just(ServerSentEvent.builder(RagChatMessageVO.builder()
                    .status("error")
                    .sessionId(request.getSessionId())
                    .messageId("safety-" + UUID.randomUUID())
                    .role("assistant")
                    .content("人工审核等待超时，请稍后重试")
                    .reviewRecordId(reviewRecordId)
                    .build()).build()));
            return Flux.concat(pendingFrame, resultFrame);
        }
        return ragService.chat(request);
    }

    @Operation(summary = "处理朗读文本")
    @PostMapping("/chat/speech-text")
    public Result<String> prepareSpeechText(@Valid @RequestBody RagSpeechTextRequest request) {
        String safeContent = safetyGatewaySupport.enforceInputText(
                SafetySourceModule.EDUCATION_RAG,
                SafetyScene.AI_OUTPUT,
                resolveGradeLevel(),
                request.getContent(),
                Map.of(
                        "entryPoint", "rag-speech-text",
                        "route", "/api/rag/chat/speech-text"
                )
        );
        return Result.setResult(HttpStatus.OK, "处理成功", ragService.prepareSpeechText(safeContent));
    }

    @Operation(summary = "获取聊天图片")
    @GetMapping("/chat/image")
    public ResponseEntity<byte[]> getChatImage(@RequestParam String objectName) {
        return ragFileAccessService.getChatImage(objectName);
    }

    private SafetyUserRole resolveUserRole() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getRoleCode() == null) {
            return SafetyUserRole.STUDENT;
        }
        String roleCode = user.getRoleCode().trim().toUpperCase();
        if ("ADMIN".equals(roleCode) || "SUPERADMIN".equals(roleCode)) {
            return SafetyUserRole.ADMIN;
        }
        if ("TEACHER".equals(roleCode)) {
            return SafetyUserRole.TEACHER;
        }
        return SafetyUserRole.STUDENT;
    }

    private SafetyGradeLevel resolveGradeLevel() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user != null) {
            SafetyGradeLevel gradeLevel = SafetyGradeLevelResolver.resolve(user.getGrade());
            if (gradeLevel != null) {
                return gradeLevel;
            }
        }
        SafetyUserRole role = resolveUserRole();
        if (role == SafetyUserRole.TEACHER || role == SafetyUserRole.ADMIN) {
            return SafetyGradeLevel.SENIOR;
        }
        return SafetyGradeLevel.JUNIOR;
    }

    private SafetyScene resolveScene() {
        SafetyUserRole role = resolveUserRole();
        if (role == SafetyUserRole.TEACHER || role == SafetyUserRole.ADMIN) {
            return SafetyScene.TEACHER_COURSE;
        }
        return SafetyScene.STUDENT_AI;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
