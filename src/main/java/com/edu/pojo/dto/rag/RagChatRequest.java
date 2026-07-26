package com.edu.pojo.dto.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class RagChatRequest {
    @NotNull(message = "sessionId 涓嶈兘涓虹┖")
    private Long sessionId;

    @NotBlank(message = "message 涓嶈兘涓虹┖")
    private String message;

    private String rewriteMessageId;

    private List<MultipartFile> imgFiles;
}
