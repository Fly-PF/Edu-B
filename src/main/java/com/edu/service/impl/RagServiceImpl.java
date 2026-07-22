package com.edu.service.impl;

import com.edu.common.properties.AIModelProperties;
import com.edu.common.properties.MinioProperties;
import com.edu.common.dto.RagTextChunkDTO;
import com.edu.common.dto.RagVectorChunkDTO;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.repository.RagRepository;
import com.edu.service.RagService;
import com.edu.util.ImageTextExtractUtil;
import com.edu.util.MdTextExtractUtil;
import com.edu.util.PdfTextExtractUtil;
import com.edu.util.PptTextExtractUtil;
import com.edu.util.SecurityUtil;
import com.edu.util.TextEmbeddingUtil;
import com.edu.util.WordTextExtractUtil;
import com.edu.util.TxtTextExtractUtil;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "pdf", "ppt", "pptx", "txt", "md", "docx", "doc");
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("ppt", Set.of("application/vnd.ms-powerpoint")),
            Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            Map.entry("txt", Set.of("text/plain")),
            Map.entry("md", Set.of("text/markdown", "text/x-markdown", "text/plain")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry("doc", Set.of("application/msword")));

    private final AIModelProperties aiModelProperties;
    private final MinioProperties minioProperties;
    private final RagRepository ragRepository;
    private final PdfTextExtractUtil pdfTextExtractUtil;
    private final PptTextExtractUtil pptTextExtractUtil;
    private final TxtTextExtractUtil txtTextExtractUtil;
    private final MdTextExtractUtil mdTextExtractUtil;
    private final WordTextExtractUtil wordTextExtractUtil;
    private final ImageTextExtractUtil imageTextExtractUtil;
    private final TextEmbeddingUtil textEmbeddingUtil;

    @Override
    public void uploadRagFile(HttpServletRequest request, MultipartFile file, Long kbId, Long docId) {
        validateUploadRequest(request, kbId, docId, file);
        List<RagTextChunkDTO> textChunks = extractText(file);
        ragRepository.insertVectorChunks(buildVectorChunks(textChunks, kbId, docId));
        uploadFile(file);
    }

    @Override
    public String chatTest(String message, List<MultipartFile> files) {
        AIModelProperties.Provider provider = aiModelProperties.getOpenai();
        if (provider == null || !StringUtils.hasText(provider.getApiKey()) || !StringUtils.hasText(provider.getBaseUrl())
                || provider.getChatModel() == null || !StringUtils.hasText(provider.getChatModel().getModelName())
                || provider.getChatModel().getModelType() == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 配置不完整，请检查 edu.ai-model.openai 相关配置");
        }

        if (hasFiles(files) && provider.getChatModel().getModelType() != AIModelProperties.ModelType.MultiModel) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "当前模型不支持图片");
        }

        OpenAiChatModel openAiChatModel = buildOpenAiChatModel(provider);
        ChatResponse response = openAiChatModel.call(new Prompt(buildUserMessage(message, files)));
        return response.getResult().getOutput().getText();
    }

    @Override
    public float[] embeddingTest(String message) {
        AIModelProperties.Provider provider = aiModelProperties.getOpenai();
        if (provider == null || !StringUtils.hasText(provider.getApiKey()) || !StringUtils.hasText(provider.getBaseUrl())
                || provider.getEmbeddingModel() == null || !StringUtils.hasText(provider.getEmbeddingModel().getModelName())) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 向量配置不完整，请检查 edu.ai-model.openai 相关配置");
        }

        OpenAiEmbeddingModel embeddingModel = buildOpenAiEmbeddingModel(provider);
        return embeddingModel.embed(message);
    }

    private OpenAiChatModel buildOpenAiChatModel(AIModelProperties.Provider provider) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(provider.getChatModel().getModelName())
                .build();

        return OpenAiChatModel.builder()
                .options(options)
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    private OpenAiEmbeddingModel buildOpenAiEmbeddingModel(AIModelProperties.Provider provider) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .apiKey(provider.getApiKey())
                .baseUrl(provider.getBaseUrl())
                .model(provider.getEmbeddingModel().getModelName())
                .build();

        return OpenAiEmbeddingModel.builder()
                .options(options)
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    private UserMessage buildUserMessage(String message, List<MultipartFile> files) {
        if (!hasFiles(files)) {
            return new UserMessage(message);
        }

        List<Media> media = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String contentType = StringUtils.hasText(file.getContentType())
                    ? file.getContentType()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            media.add(new Media(MediaType.parseMediaType(contentType), file.getResource()));
        }

        if (media.isEmpty()) {
            return new UserMessage(message);
        }

        return UserMessage.builder()
                .text(message)
                .media(media)
                .build();
    }

    private boolean hasFiles(List<MultipartFile> files) {
        return files != null && files.stream().anyMatch(file -> file != null && !file.isEmpty());
    }

    private void validateRagFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件不能为空");
        }

        DataSize maxRagFileSize = minioProperties.getRag().getMaxRagFileSize();
        if (maxRagFileSize == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "RAG文件大小限制未配置");
        }
        if (file.getSize() > maxRagFileSize.toBytes()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件不能超过" + maxRagFileSize.toMegabytes() + "MB");
        }

        String extension = getExtension(file);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅支持jpg、jpeg、png、webp、pdf、ppt、pptx、txt、md、docx、doc格式文件");
        }

        String contentType = file.getContentType();
        Set<String> allowedContentTypes = ALLOWED_CONTENT_TYPES.get(extension);
        if (StringUtils.hasText(contentType)
                && allowedContentTypes != null
                && !allowedContentTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件类型与文件后缀不匹配");
        }
    }

    private List<RagTextChunkDTO> extractText(MultipartFile file) {
        String extension = getExtension(file);
        if (isImage(extension)) {
            try (InputStream inputStream = file.getInputStream()) {
                String content = imageTextExtractUtil.extract(inputStream);
                return List.of(new RagTextChunkDTO("image 1/1", content));
            } catch (BaseException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "图片文字提取失败");
            }
        }

        try (InputStream inputStream = file.getInputStream()) {
            return switch (extension) {
                case "pdf" -> pdfTextExtractUtil.extract(inputStream);
                case "ppt", "pptx" -> pptTextExtractUtil.extract(inputStream);
                case "txt" -> txtTextExtractUtil.extract(inputStream);
                case "md" -> mdTextExtractUtil.extract(inputStream);
                case "docx", "doc" -> wordTextExtractUtil.extract(inputStream, extension);
                default -> throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该文件文字提取");
            };
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "文件文字提取失败");
        }
    }

    private List<RagVectorChunkDTO> buildVectorChunks(List<RagTextChunkDTO> textChunks, Long kbId, Long docId) {
        List<RagVectorChunkDTO> vectorChunks = new ArrayList<>();
        for (RagTextChunkDTO textChunk : textChunks) {
            if (textChunk == null || !StringUtils.hasText(textChunk.getContent())) {
                continue;
            }
            vectorChunks.add(new RagVectorChunkDTO(kbId, docId, textChunk.getSourceInfo(), textChunk.getContent(),
                    textEmbeddingUtil.embed(textChunk.getContent()).getVector()));
        }
        return vectorChunks;
    }

    private void validateUploadRequest(HttpServletRequest request, Long kbId, Long docId, MultipartFile file) {
        validateSingleFile(request);
        validateRagIds(kbId, docId);
        validateRagFile(file);
    }

    private void uploadFile(MultipartFile file) {
        UserInfoDTO loginUser = getLoginUser();
        ragRepository.uploadObject(file, buildObjectName(loginUser.getUserId(), file));
    }

    private UserInfoDTO getLoginUser() {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return loginUser;
    }

    private void validateRagIds(Long kbId, Long docId) {
        if (kbId == null || kbId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "kb_id必须为正数");
        }
        if (docId == null || docId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "doc_id必须为正数");
        }
    }

    private boolean isImage(String extension) {
        return switch (extension) {
            case "jpg", "jpeg", "png", "webp" -> true;
            default -> false;
        };
    }

    private void validateSingleFile(HttpServletRequest request) {
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            long fileCount = multipartRequest.getMultiFileMap().values().stream()
                    .mapToLong(List::size)
                    .sum();
            if (fileCount != 1) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "一次只能上传一个文件");
            }
        }
    }

    private String buildObjectName(Long userId, MultipartFile file) {
        String extension = getExtension(file);
        return getRagFilesBaseUrl() + userId + "/" + getFileType(extension) + "/" + UUID.randomUUID() + "." + extension;
    }

    private String getFileType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg", "png", "webp" -> "img";
            default -> extension;
        };
    }

    private String getExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String getRagFilesBaseUrl() {
        String ragFilesBaseUrl = minioProperties.getRag().getRagFilesBaseUrl();
        if (!StringUtils.hasText(ragFilesBaseUrl)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "RAG文件存储路径未配置");
        }
        ragFilesBaseUrl = trimStartSlash(ragFilesBaseUrl);
        return ragFilesBaseUrl.endsWith("/") ? ragFilesBaseUrl : ragFilesBaseUrl + "/";
    }

    private String trimStartSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
