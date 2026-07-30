package com.edu.service.impl;

import com.edu.common.dto.RagTextChunkDTO;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.RagDocumentPO;
import com.edu.pojo.po.RagKnowledgeBasePO;
import com.edu.pojo.vo.rag.RagFilePreviewContentVO;
import com.edu.pojo.vo.rag.RagFilePreviewImageVO;
import com.edu.pojo.vo.rag.RagFilePreviewImagesVO;
import com.edu.repository.RagDocumentRepository;
import com.edu.repository.RagKnowledgeBaseRepository;
import com.edu.service.RagFileAccessService;
import com.edu.service.RagService;
import com.edu.util.MdTextExtractUtil;
import com.edu.util.PdfTextExtractUtil;
import com.edu.util.PptTextExtractUtil;
import com.edu.util.SecurityUtil;
import com.edu.util.TxtTextExtractUtil;
import com.edu.util.WordHtmlPreviewUtil;
import com.edu.util.WordTextExtractUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagFileAccessServiceImpl implements RagFileAccessService {
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> PREVIEWABLE_FILE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "pdf", "ppt", "pptx", "txt", "md", "docx", "doc");

    private final RagService ragService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final RagDocumentRepository ragDocumentRepository;
    private final RagKnowledgeBaseRepository ragKnowledgeBaseRepository;
    private final PdfTextExtractUtil pdfTextExtractUtil;
    private final PptTextExtractUtil pptTextExtractUtil;
    private final TxtTextExtractUtil txtTextExtractUtil;
    private final MdTextExtractUtil mdTextExtractUtil;
    private final WordTextExtractUtil wordTextExtractUtil;
    private final WordHtmlPreviewUtil wordHtmlPreviewUtil;

    @Override
    public ResponseEntity<byte[]> getKnowledgeBaseCover(String objectName) {
        validateImageObjectName(objectName, false);
        try {
            return inlineImageResponse(objectName, readObject(objectName));
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "图片不存在");
        }
    }

    @Override
    public ResponseEntity<byte[]> getChatImage(String objectName) {
        validateImageObjectName(objectName, true);
        if (!ragService.existsChatImage(objectName)) {
            throw new BaseException(HttpStatus.NOT_FOUND, "图片不存在");
        }
        try {
            return inlineImageResponse(objectName, readObject(objectName));
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "图片不存在");
        }
    }

    @Override
    public ResponseEntity<byte[]> previewKnowledgeBaseDocument(Long kbId, String fileUrl) {
        validatePreviewFileUrl(fileUrl);
        validatePreviewDocument(kbId, fileUrl);
        try {
            return ResponseEntity.ok()
                    .contentType(getMediaType(fileUrl))
                    .cacheControl(CacheControl.noCache())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + getDisplayFileName(fileUrl) + "\"")
                    .body(readObject(fileUrl));
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }
    }

    @Override
    public RagFilePreviewContentVO previewKnowledgeBaseDocumentContent(Long kbId, String fileUrl) {
        validatePreviewFileUrl(fileUrl);
        RagDocumentPO document = validatePreviewDocument(kbId, fileUrl);
        String extension = getExtension(fileUrl);
        try {
            byte[] bytes = readObject(fileUrl);
            List<RagTextChunkDTO> chunks = extractPreviewText(new ByteArrayInputStream(bytes), extension);
            String content = chunks.stream()
                    .map(RagTextChunkDTO::getContent)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
            String html = isWordExtension(extension)
                    ? wordHtmlPreviewUtil.renderHtml(new ByteArrayInputStream(bytes), extension)
                    : "";
            return new RagFilePreviewContentVO(document.getDocName(), extension, chunks, content, html);
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }
    }

    @Override
    public RagFilePreviewImagesVO previewKnowledgeBaseDocumentImages(Long kbId, String fileUrl) {
        validatePreviewFileUrl(fileUrl);
        RagDocumentPO document = validatePreviewDocument(kbId, fileUrl);
        String extension = getExtension(fileUrl);
        try {
            List<byte[]> pageImages = extractPreviewImages(new ByteArrayInputStream(readObject(fileUrl)), extension);
            List<RagFilePreviewImageVO> pages = new ArrayList<>(pageImages.size());
            for (int i = 0; i < pageImages.size(); i++) {
                pages.add(new RagFilePreviewImageVO(i + 1, toDataUrl(pageImages.get(i))));
            }
            return new RagFilePreviewImagesVO(document.getDocName(), extension, pages);
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }
    }

    private ResponseEntity<byte[]> inlineImageResponse(String objectName, byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(getMediaType(objectName))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(bytes);
    }

    private byte[] readObject(String objectName) throws Exception {
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(getBucketName())
                .object(objectName)
                .build())) {
            return StreamUtils.copyToByteArray(inputStream);
        }
    }

    private void validateImageObjectName(String objectName, boolean mustBeRagFile) {
        boolean invalidObjectName = !StringUtils.hasText(objectName) || objectName.contains("..");
        boolean invalidRagFilePath = mustBeRagFile && !objectName.startsWith(minioProperties.getRag().getRagFilesBaseUrl());
        if (invalidObjectName || invalidRagFilePath) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "图片地址错误");
        }

        if (!IMAGE_EXTENSIONS.contains(getExtension(objectName))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅支持jpg、jpeg、png、webp格式图片");
        }
    }

    private void validatePreviewFileUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || fileUrl.contains("..")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件地址错误");
        }

        if (!PREVIEWABLE_FILE_EXTENSIONS.contains(getExtension(fileUrl))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该文件预览");
        }
    }

    private RagDocumentPO validatePreviewDocument(Long kbId, String fileUrl) {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        RagKnowledgeBasePO publicKnowledgeBase = ragKnowledgeBaseRepository.selectPublicKnowledgeBaseById(kbId);
        RagKnowledgeBasePO myKnowledgeBase = loginUser == null || loginUser.getUserId() == null
                ? null
                : ragKnowledgeBaseRepository.selectKnowledgeBaseById(kbId, loginUser.getUserId());
        if (publicKnowledgeBase == null && myKnowledgeBase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "知识库不存在");
        }

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

    private MediaType getMediaType(String objectName) {
        return switch (getExtension(objectName)) {
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
}
