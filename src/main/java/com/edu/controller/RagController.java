package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.common.dto.RagTextChunkDTO;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.pojo.po.RagDocumentPO;
import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import com.edu.pojo.vo.rag.RagDocumentVO;
import com.edu.repository.RagDocumentRepository;
import com.edu.service.RagService;
import com.edu.util.MdTextExtractUtil;
import com.edu.util.PdfTextExtractUtil;
import com.edu.util.PptTextExtractUtil;
import com.edu.util.TxtTextExtractUtil;
import com.edu.util.WordHtmlPreviewUtil;
import com.edu.util.WordTextExtractUtil;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
@Validated
@Tag(name = "RAG")
public class RagController {
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_RAG_FILE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "pdf", "ppt", "pptx", "txt", "md", "docx", "doc");

    private final RagService ragService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final RagDocumentRepository ragDocumentRepository;
    private final PdfTextExtractUtil pdfTextExtractUtil;
    private final PptTextExtractUtil pptTextExtractUtil;
    private final TxtTextExtractUtil txtTextExtractUtil;
    private final MdTextExtractUtil mdTextExtractUtil;
    private final WordTextExtractUtil wordTextExtractUtil;
    private final WordHtmlPreviewUtil wordHtmlPreviewUtil;

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

    @Operation(summary = "分页查询知识库文档")
    @GetMapping("/kb/documents")
    public Result<PageResult<RagDocumentVO>> pageKnowledgeBaseDocuments(@RequestParam("kb_id") @NotNull @Min(1) Long kbId,
                                                                        @RequestParam(required = false) Integer pageNum,
                                                                        @RequestParam(required = false) Integer pageSize,
                                                                        @RequestParam(value = "doc_type", required = false) String docType,
                                                                        @RequestParam(value = "doc_name", required = false) String docName) {
        return Result.setResult(HttpStatus.OK, "查询成功",
                ragService.pageKnowledgeBaseDocuments(kbId, pageNum, pageSize, docType, docName));
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

    @Operation(summary = "获取RAG文件预览")
    @GetMapping("/files/preview")
    public ResponseEntity<byte[]> previewRagFile(@RequestParam("file_url") String fileUrl,
                                                 @RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        validateRagFileUrl(fileUrl);
        validatePreviewDocument(kbId, fileUrl);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(getBucketName())
                .object(fileUrl)
                .build())) {
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
            return ResponseEntity.ok()
                    .contentType(getMediaType(fileUrl))
                    .cacheControl(CacheControl.noCache())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + getDisplayFileName(fileUrl) + "\"")
                    .body(bytes);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }
    }

    @Operation(summary = "获取RAG文件预览文本")
    @GetMapping("/files/preview-content")
    public Result<RagFilePreviewContentVO> previewRagFileContent(@RequestParam("file_url") String fileUrl,
                                                                 @RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        validateRagFileUrl(fileUrl);
        RagDocumentPO document = validatePreviewDocument(kbId, fileUrl);
        String extension = getExtension(fileUrl);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(getBucketName())
                .object(fileUrl)
                .build())) {
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
            List<RagTextChunkDTO> chunks = extractPreviewText(new ByteArrayInputStream(bytes), extension);
            String content = chunks.stream()
                    .map(RagTextChunkDTO::getContent)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
            String html = isWordExtension(extension) ? wordHtmlPreviewUtil.renderHtml(new ByteArrayInputStream(bytes), extension) : "";
            RagFilePreviewContentVO data = new RagFilePreviewContentVO(document.getDocName(), extension, chunks, content, html);
            return Result.setResult(HttpStatus.OK, "查询成功", data);
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }
    }

    @Operation(summary = "获取RAG文件预览图片")
    @GetMapping("/files/preview-images")
    public Result<RagFilePreviewImagesVO> previewRagFileImages(@RequestParam("file_url") String fileUrl,
                                                               @RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        validateRagFileUrl(fileUrl);
        RagDocumentPO document = validatePreviewDocument(kbId, fileUrl);
        String extension = getExtension(fileUrl);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(getBucketName())
                .object(fileUrl)
                .build())) {
            List<byte[]> pageImages = extractPreviewImages(inputStream, extension);
            List<RagFilePreviewImageVO> pages = new ArrayList<>(pageImages.size());
            for (int i = 0; i < pageImages.size(); i++) {
                pages.add(new RagFilePreviewImageVO(i + 1, toDataUrl(pageImages.get(i))));
            }
            return Result.setResult(HttpStatus.OK, "查询成功",
                    new RagFilePreviewImagesVO(document.getDocName(), extension, pages));
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
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

    @Operation(summary = "更新RAG文件信息")
    @PostMapping("/files/update")
    public Result<Void> updateRagDocument(@RequestParam("kb_id") @NotNull @Min(1) Long kbId,
                                          @RequestParam("doc_id") @NotNull @Min(1) Long docId,
                                          @RequestParam("doc_name") @NotBlank String docName,
                                          @RequestParam(required = false) String description) {
        ragService.updateRagDocument(kbId, docId, docName, description);
        return Result.setResult(HttpStatus.OK, "更新成功");
    }

    @Operation(summary = "删除RAG文件")
    @PostMapping("/files/delete")
    public Result<Void> deleteRagDocument(@RequestParam("kb_id") @NotNull @Min(1) Long kbId,
                                          @RequestParam("doc_id") @NotNull @Min(1) Long docId) {
        ragService.deleteRagDocument(kbId, docId);
        return Result.setResult(HttpStatus.OK, "删除成功");
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

    private void validateRagFileUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || fileUrl.contains("..")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件地址错误");
        }

        String extension = StringUtils.getFilenameExtension(fileUrl);
        if (extension == null || !ALLOWED_RAG_FILE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该文件预览");
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
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "txt" -> MediaType.TEXT_PLAIN;
            case "md" -> MediaType.TEXT_MARKDOWN;
            case "doc" -> MediaType.parseMediaType("application/msword");
            case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "ppt" -> MediaType.parseMediaType("application/vnd.ms-powerpoint");
            case "pptx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private RagDocumentPO validatePreviewDocument(Long kbId, String fileUrl) {
        ragService.getMyKnowledgeBase(kbId);
        RagDocumentPO document = ragDocumentRepository.selectKnowledgeBaseDocument(kbId, fileUrl);
        if (document == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        return document;
    }

    private List<RagTextChunkDTO> extractPreviewText(InputStream inputStream, String extension) {
        return switch (extension) {
            case "pdf" -> pdfTextExtractUtil.extract(inputStream);
            case "ppt", "pptx" -> pptTextExtractUtil.extract(inputStream);
            case "txt" -> txtTextExtractUtil.extract(inputStream);
            case "md" -> mdTextExtractUtil.extract(inputStream);
            case "docx", "doc" -> wordTextExtractUtil.extract(inputStream, extension);
            default -> throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该文件文本预览");
        };
    }

    private List<byte[]> extractPreviewImages(InputStream inputStream, String extension) {
        return switch (extension) {
            case "pdf" -> pdfTextExtractUtil.renderPages(inputStream);
            case "ppt", "pptx" -> pptTextExtractUtil.renderPages(inputStream);
            default -> throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该文件图片预览");
        };
    }

    private boolean isWordExtension(String extension) {
        return "doc".equals(extension) || "docx".equals(extension);
    }

    private String toDataUrl(byte[] bytes) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String getExtension(String objectName) {
        String extension = StringUtils.getFilenameExtension(objectName);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String getDisplayFileName(String fileUrl) {
        String fileName = StringUtils.getFilename(fileUrl);
        return StringUtils.hasText(fileName) ? fileName : "file";
    }

    private String getBucketName() {
        String bucketName = minioProperties.getBuckerName();
        if (!StringUtils.hasText(bucketName)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO存储桶未配置");
        }
        return bucketName;
    }

    public record RagFilePreviewContentVO(String fileName, String extension, List<RagTextChunkDTO> chunks,
                                          String content, String html) {
    }

    public record RagFilePreviewImageVO(Integer pageNum, String imageUrl) {
    }

    public record RagFilePreviewImagesVO(String fileName, String extension, List<RagFilePreviewImageVO> pages) {
    }
}
