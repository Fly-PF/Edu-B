package com.edu.controller;

import com.edu.common.Result;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import com.edu.service.RagService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
@Validated
@Tag(name = "RAG")
public class RagController {
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final RagService ragService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Operation(summary = "查询我的知识库")
    @GetMapping("/kb/my")
    public Result<List<RagKnowledgeBaseVO>> listMyKnowledgeBases(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) Integer status,
                                                                 @RequestParam(value = "is_public", required = false) Integer isPublic,
                                                                 @RequestParam(value = "kb_type", required = false) Integer kbType) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.listMyKnowledgeBases(keyword, status, isPublic, kbType));
    }

    @Operation(summary = "获取我的知识库详情")
    @GetMapping("/kb/my/detail")
    public Result<RagKnowledgeBaseVO> getMyKnowledgeBase(@RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.getMyKnowledgeBase(kbId));
    }

    @Operation(summary = "创建知识库")
    @PostMapping("/kb")
    public Result<Void> createKnowledgeBase(@RequestParam("kb_name") String kbName,
                                            @RequestParam(value = "description", required = false) String description,
                                            @RequestParam("kb_type") Integer kbType,
                                            @RequestParam(value = "is_public") Integer isPublic,
                                            @RequestPart(value = "kb_cover") MultipartFile file) {
        ragService.createKnowledgeBase(kbName, description, kbType, isPublic, file);
        return Result.setResult(HttpStatus.CREATED, "创建成功");
    }

    @Operation(summary = "更新知识库")
    @PostMapping("/kb/update")
    public Result<Void> updateKnowledgeBase(@RequestParam("kb_id") @NotNull @Min(1) Long kbId,
                                            @RequestParam("kb_name") String kbName,
                                            @RequestParam(value = "description", required = false) String description,
                                            @RequestParam("kb_type") Integer kbType,
                                            @RequestParam("is_public") Integer isPublic,
                                            @RequestParam("status") Integer status,
                                            @RequestPart(value = "kb_cover", required = false) MultipartFile file) {
        ragService.updateKnowledgeBase(kbId, kbName, description, kbType, isPublic, status, file);
        return Result.setResult(HttpStatus.OK, "更新成功");
    }

    @Operation(summary = "获取知识库封面")
    @GetMapping("/kb/cover")
    public ResponseEntity<byte[]> getKnowledgeBaseCover(@RequestParam String objectName) {
        validateCoverObjectName(objectName);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(getBucketName())
                .object(objectName)
                .build())) {
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
            return ResponseEntity.ok()
                    .contentType(getMediaType(objectName))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(bytes);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "图片不存在");
        }
    }

    @Operation(summary = "上传RAG文件")
    @PostMapping("/files/upload")
    public Result<Void> uploadRagFile(HttpServletRequest request,
                                      @RequestPart MultipartFile file,
                                      @RequestParam(required = false) String description,
                                      @RequestParam @NotNull @Min(1) Long kbId) {
        ragService.uploadRagFile(request, file, description, kbId);
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

    private void validateCoverObjectName(String objectName) {
        if (!StringUtils.hasText(objectName) || objectName.contains("..")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "图片地址错误");
        }

        String extension = StringUtils.getFilenameExtension(objectName);
        if (extension == null || !ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅支持jpg、jpeg、png、webp格式图片");
        }
    }

    private MediaType getMediaType(String objectName) {
        String extension = StringUtils.getFilenameExtension(objectName);
        if (extension == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private String getBucketName() {
        String bucketName = minioProperties.getBuckerName();
        if (!StringUtils.hasText(bucketName)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO存储桶未配置");
        }
        return bucketName;
    }
}
