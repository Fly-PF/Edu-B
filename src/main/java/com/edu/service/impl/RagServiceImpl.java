package com.edu.service.impl;

import com.edu.common.dto.RagTextChunkDTO;
import com.edu.common.dto.RagVectorChunkDTO;
import com.edu.common.properties.AIModelProperties;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.RagDocumentPO;
import com.edu.pojo.po.RagKnowledgeBasePO;
import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import com.edu.repository.RagDocumentRepository;
import com.edu.repository.RagKnowledgeBaseRepository;
import com.edu.repository.RagRepository;
import com.edu.service.RagService;
import com.edu.util.ImageTextExtractUtil;
import com.edu.util.MdTextExtractUtil;
import com.edu.util.PdfTextExtractUtil;
import com.edu.util.PptTextExtractUtil;
import com.edu.util.SecurityUtil;
import com.edu.util.TextEmbeddingUtil;
import com.edu.util.TxtTextExtractUtil;
import com.edu.util.WordTextExtractUtil;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {
    private static final Set<String> COVER_ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> COVER_ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
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
    private final RagDocumentRepository ragDocumentRepository;
    private final RagRepository ragRepository;
    private final RagKnowledgeBaseRepository ragKnowledgeBaseRepository;
    private final PdfTextExtractUtil pdfTextExtractUtil;
    private final PptTextExtractUtil pptTextExtractUtil;
    private final TxtTextExtractUtil txtTextExtractUtil;
    private final MdTextExtractUtil mdTextExtractUtil;
    private final WordTextExtractUtil wordTextExtractUtil;
    private final ImageTextExtractUtil imageTextExtractUtil;
    private final TextEmbeddingUtil textEmbeddingUtil;

    @Override
    public List<RagKnowledgeBaseVO> listMyKnowledgeBases(String keyword, Integer status, Integer isPublic, Integer kbType) {
        UserInfoDTO loginUser = getLoginUser();
        validateKnowledgeBaseFilters(status, isPublic, kbType);
        return ragKnowledgeBaseRepository.selectUserKnowledgeBases(loginUser.getUserId(), keyword, status, isPublic, kbType)
                .stream()
                .map(this::toKnowledgeBaseVO)
                .toList();
    }

    @Override
    public RagKnowledgeBaseVO getMyKnowledgeBase(Long kbId) {
        UserInfoDTO loginUser = getLoginUser();
        if (kbId == null || kbId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库ID无效");
        }

        RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectKnowledgeBaseById(kbId, loginUser.getUserId());
        if (knowledgeBase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "知识库不存在");
        }

        return toKnowledgeBaseVO(knowledgeBase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createKnowledgeBase(String kbName, String description, Integer kbType, Integer isPublic, MultipartFile file) {
        UserInfoDTO loginUser = getLoginUser();
        validateKnowledgeBase(kbName, kbType, isPublic, file);

        String coverObjectName = buildCoverObjectName(loginUser.getUserId(), file);
        ragRepository.uploadObject(file, coverObjectName);

        RagKnowledgeBasePO knowledgeBase = RagKnowledgeBasePO.builder()
                .userId(loginUser.getUserId())
                .kbName(kbName.trim())
                .kbCover(coverObjectName)
                .description(StringUtils.hasText(description) ? description.trim() : null)
                .kbType(kbType)
                .publicFlag(isPublic)
                .status(1)
                .deleted(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        try {
            if (ragKnowledgeBaseRepository.insertKnowledgeBase(knowledgeBase) != 1) {
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "知识库创建失败");
            }
        } catch (RuntimeException ex) {
            if (StringUtils.hasText(coverObjectName)) {
                ragRepository.deleteObject(coverObjectName);
            }
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeBase(Long kbId, String kbName, String description, Integer kbType, Integer isPublic,
                                    Integer status, MultipartFile file) {
        UserInfoDTO loginUser = getLoginUser();
        validateKnowledgeBaseId(kbId);
        validateKnowledgeBaseUpdate(kbName, kbType, isPublic, status);

        RagKnowledgeBasePO origin = ragKnowledgeBaseRepository.selectKnowledgeBaseById(kbId, loginUser.getUserId());
        if (origin == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "知识库不存在");
        }

        String coverObjectName = origin.getKbCover();
        if (file != null && !file.isEmpty()) {
            validateCoverFile(file);
            String newCoverObjectName = buildCoverObjectName(loginUser.getUserId(), file);
            ragRepository.uploadObject(file, newCoverObjectName);
            coverObjectName = newCoverObjectName;
        }

        RagKnowledgeBasePO knowledgeBase = RagKnowledgeBasePO.builder()
                .id(origin.getId())
                .userId(origin.getUserId())
                .kbName(kbName.trim())
                .kbCover(coverObjectName)
                .description(StringUtils.hasText(description) ? description.trim() : null)
                .kbType(kbType)
                .publicFlag(isPublic)
                .status(status)
                .deleted(0)
                .updateTime(LocalDateTime.now())
                .build();

        try {
            if (ragKnowledgeBaseRepository.updateKnowledgeBase(knowledgeBase) != 1) {
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "知识库更新失败");
            }
        } catch (RuntimeException ex) {
            if (file != null && !file.isEmpty()) {
                ragRepository.deleteObject(coverObjectName);
            }
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRagFile(HttpServletRequest request, MultipartFile file, String description, Long kbId) {
        validateUploadRequest(request, kbId, file);
        validateKnowledgeBaseForUpload(kbId);
        List<RagTextChunkDTO> textChunks = extractText(file);
        UserInfoDTO loginUser = getLoginUser();
        String objectName = buildObjectName(loginUser.getUserId(), file);

        uploadFile(file, objectName);

        RagDocumentPO document = buildDocument(kbId, file, description, objectName);
        try {
            if (ragDocumentRepository.insertDocument(document) != 1 || document.getId() == null) {
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "RAG文档保存失败");
            }

            ragRepository.insertVectorChunks(buildVectorChunks(textChunks, kbId, document.getId()));
        } catch (RuntimeException ex) {
            RuntimeException rollbackEx = rollbackUploadedRag(document, kbId, objectName);
            if (rollbackEx != null) {
                ex.addSuppressed(rollbackEx);
            }
            throw ex;
        }
    }

    @Override
    public String chatTest(String message, List<MultipartFile> files) {
        AIModelProperties.Provider provider = aiModelProperties.getOpenai();
        if (provider == null || !StringUtils.hasText(provider.getApiKey()) || !StringUtils.hasText(provider.getBaseUrl())
                || provider.getChatModel() == null || !StringUtils.hasText(provider.getChatModel().getModelName())
                || provider.getChatModel().getModelType() == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "AI配置不完整，请检查 edu.ai-model.openai 相关配置");
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
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "AI向量配置不完整，请检查 edu.ai-model.openai 相关配置");
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
                default -> throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该文件文本提取");
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

    private void validateUploadRequest(HttpServletRequest request, Long kbId, MultipartFile file) {
        validateSingleFile(request);
        validateKbId(kbId);
        validateRagFile(file);
    }

    private void uploadFile(MultipartFile file, String objectName) {
        ragRepository.uploadObject(file, objectName);
    }

    private UserInfoDTO getLoginUser() {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return loginUser;
    }

    private void validateKbId(Long kbId) {
        if (kbId == null || kbId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "kb_id必须为正数");
        }
    }

    private void validateKnowledgeBaseForUpload(Long kbId) {
        RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectKnowledgeBaseById(kbId);
        if (knowledgeBase == null || knowledgeBase.getDeleted() == null || knowledgeBase.getDeleted() == 1) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "该知识库不存在！");
        }
        if (knowledgeBase.getStatus() != null && knowledgeBase.getStatus() == 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "该知识库已被禁用，请启用后添加！");
        }
    }

    private RagDocumentPO buildDocument(Long kbId, MultipartFile file, String description, String fileUrl) {
        String docName = getOriginalFileName(file);
        String docType = getDocumentType(docName);
        LocalDateTime now = LocalDateTime.now();
        return RagDocumentPO.builder()
                .kbId(kbId)
                .docName(docName)
                .docType(docType)
                .description(StringUtils.hasText(description) ? description.trim() : null)
                .fileUrl(fileUrl)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
    }

    private RuntimeException rollbackUploadedRag(RagDocumentPO document, Long kbId, String objectName) {
        RuntimeException rollbackEx = null;
        if (document != null && document.getId() != null) {
            try {
                ragRepository.deleteVectorChunks(kbId, document.getId());
            } catch (RuntimeException ex) {
                rollbackEx = ex;
            }
            try {
                ragDocumentRepository.deleteDocumentById(document.getId());
            } catch (RuntimeException ex) {
                if (rollbackEx == null) {
                    rollbackEx = ex;
                } else {
                    rollbackEx.addSuppressed(ex);
                }
            }
        }

        try {
            ragRepository.deleteObjectStrict(objectName);
        } catch (RuntimeException ex) {
            if (rollbackEx == null) {
                rollbackEx = ex;
            } else {
                rollbackEx.addSuppressed(ex);
            }
        }
        return rollbackEx;
    }

    private String getOriginalFileName(MultipartFile file) {
        String originalFileName = StringUtils.getFilename(file.getOriginalFilename());
        if (!StringUtils.hasText(originalFileName)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "RAG文件名不能为空");
        }
        return originalFileName;
    }

    private String getDocumentType(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "RAG文件后缀不能为空");
        }
        return "." + extension.toLowerCase(Locale.ROOT);
    }

    private boolean isImage(String extension) {
        return switch (extension) {
            case "jpg", "jpeg", "png", "webp" -> true;
            default -> false;
        };
    }

    private void validateKnowledgeBase(String kbName, Integer kbType, Integer isPublic, MultipartFile file) {
        if (!StringUtils.hasText(kbName)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库名称不能为空");
        }
        if (kbType == null || kbType < 1 || kbType > 4) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库类型无效");
        }
        if (isPublic == null || isPublic != 0 && isPublic != 1) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "公开状态无效");
        }
        validateCoverFile(file);
    }

    private void validateKnowledgeBaseId(Long kbId) {
        if (kbId == null || kbId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库ID无效");
        }
    }

    private void validateKnowledgeBaseUpdate(String kbName, Integer kbType, Integer isPublic, Integer status) {
        if (!StringUtils.hasText(kbName)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库名称不能为空");
        }
        if (kbType == null || kbType < 1 || kbType > 4) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库类型无效");
        }
        if (isPublic == null || isPublic != 0 && isPublic != 1) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "公开状态无效");
        }
        if (status == null || status != 0 && status != 1) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库状态无效");
        }
    }

    private void validateKnowledgeBaseFilters(Integer status, Integer isPublic, Integer kbType) {
        if (status != null && status != 0 && status != 1) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库状态无效");
        }
        if (isPublic != null && isPublic != 0 && isPublic != 1) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "公开状态无效");
        }
        if (kbType != null && (kbType < 1 || kbType > 4)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库类型无效");
        }
    }

    private void validateCoverFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "封面图不能为空");
        }

        String coverFileName = StringUtils.getFilename(file.getOriginalFilename());
        if (!StringUtils.hasText(coverFileName)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "封面文件名不能为空");
        }

        String extension = StringUtils.getFilenameExtension(coverFileName);
        if (!StringUtils.hasText(extension) || !COVER_ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "封面图仅支持jpg、jpeg、png、webp格式");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !COVER_ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "封面图仅支持jpg、jpeg、png、webp格式");
        }
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

    private String buildCoverObjectName(Long userId, MultipartFile file) {
        return getRagFilesBaseUrl() + userId + "/cover/" + UUID.randomUUID() + "." + getExtension(file);
    }

    private RagKnowledgeBaseVO toKnowledgeBaseVO(RagKnowledgeBasePO knowledgeBase) {
        return RagKnowledgeBaseVO.builder()
                .id(knowledgeBase.getId())
                .kbName(knowledgeBase.getKbName())
                .kbCover(knowledgeBase.getKbCover())
                .description(knowledgeBase.getDescription())
                .kbType(knowledgeBase.getKbType())
                .publicFlag(knowledgeBase.getPublicFlag())
                .status(knowledgeBase.getStatus())
                .build();
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
