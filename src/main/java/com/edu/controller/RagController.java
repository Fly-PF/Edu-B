package com.edu.controller;

import com.edu.common.Result;
import com.edu.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
@Tag(name = "RAG")
public class RagController {
    private final RagService ragService;

    @Operation(summary = "上传RAG文件")
    @PostMapping("/files/upload")
    public Result<Void> uploadRagFile(HttpServletRequest request, @RequestPart("file") MultipartFile file) {
        ragService.uploadRagFile(request, file);
        return Result.setResult(HttpStatus.OK, "上传成功");
    }

    @Operation(summary = "测试AI聊天接口")
    @PostMapping(value = "/chat/test")
    public Result<String> chatTest(@Valid @RequestParam String message,
                                   @RequestParam(required = false) List<MultipartFile> files) {
        return Result.setResult(HttpStatus.OK, "success", ragService.chatTest(message, files));
    }

    @Operation(summary = "测试OpenAI向量接口")
    @PostMapping("/embedding/test")
    public Result<float[]> embeddingTest(@Valid @RequestBody ChatTestReq request) {
        return Result.setResult(HttpStatus.OK, "success", ragService.embeddingTest(request.getMessage()));
    }

    public static class ChatTestReq {
        @NotBlank(message = "message 不能为空")
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
