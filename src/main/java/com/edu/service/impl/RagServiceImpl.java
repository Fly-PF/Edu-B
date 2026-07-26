package com.edu.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.common.dto.RagTextChunkDTO;
import com.edu.common.dto.RagVectorChunkDTO;
import com.edu.common.properties.AIModelProperties;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.rag.RagChatRequest;
import com.edu.pojo.dto.rag.RagChatSessionCreateRequest;
import com.edu.pojo.dto.rag.RagChatSessionRenameRequest;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.RagChatMessagePO;
import com.edu.pojo.po.RagChatSessionPO;
import com.edu.pojo.po.RagDocumentPO;
import com.edu.pojo.po.RagKbUserCollectionPO;
import com.edu.pojo.po.RagKnowledgeBasePO;
import com.edu.pojo.po.RagMsgDocRefPO;
import com.edu.pojo.po.RagSessionKbRefPO;
import com.edu.pojo.vo.rag.RagChatDocRefVO;
import com.edu.pojo.vo.rag.RagChatMessageVO;
import com.edu.pojo.vo.rag.RagChatSessionVO;
import com.edu.pojo.vo.rag.RagDocumentVO;
import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import com.edu.repository.RagChatMessageRepository;
import com.edu.repository.RagChatSessionRepository;
import com.edu.repository.RagDocumentRepository;
import com.edu.repository.RagKbUserCollectionRepository;
import com.edu.repository.RagKnowledgeBaseRepository;
import com.edu.repository.RagMsgDocRefRepository;
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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {
    private static final String RAG_FILE_UPLOAD_EXTRACT_FLAG_PREFIX = "rag_file_upload_extract_flag_";
    private static final long RAG_FILE_UPLOAD_EXTRACT_FLAG_EXPIRE_MINUTES = 5L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
    private final RagChatMessageRepository ragChatMessageRepository;
    private final RagChatSessionRepository ragChatSessionRepository;
    private final RagRepository ragRepository;
    private final RagMsgDocRefRepository ragMsgDocRefRepository;
    private final RagKbUserCollectionRepository ragKbUserCollectionRepository;
    private final RagKnowledgeBaseRepository ragKnowledgeBaseRepository;
    private final PdfTextExtractUtil pdfTextExtractUtil;
    private final PptTextExtractUtil pptTextExtractUtil;
    private final TxtTextExtractUtil txtTextExtractUtil;
    private final MdTextExtractUtil mdTextExtractUtil;
    private final WordTextExtractUtil wordTextExtractUtil;
    private final ImageTextExtractUtil imageTextExtractUtil;
    private final TextEmbeddingUtil textEmbeddingUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

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
    public List<RagKnowledgeBaseVO> listPublicKnowledgeBases(Integer kbType, Integer limit) {
        validatePublicKnowledgeBaseQuery(kbType, limit);
        return ragKnowledgeBaseRepository.selectPublicKnowledgeBases(kbType, limit)
                .stream()
                .map(this::toKnowledgeBaseVO)
                .toList();
    }

    @Override
    public PageResult<RagKnowledgeBaseVO> pagePublicKnowledgeBases(String keyword, Integer kbType, Integer pageNum,
                                                                   Integer pageSize) {
        validatePublicKnowledgeBasePageQuery(kbType);
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<RagKnowledgeBasePO> page = ragKnowledgeBaseRepository.selectPublicKnowledgeBasePage(
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                keyword,
                kbType
        );
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream().map(this::toKnowledgeBaseVO).toList());
    }

    @Override
    public PageResult<RagKnowledgeBaseVO> pageCollectedKnowledgeBases(String keyword, Integer kbType, Integer pageNum,
                                                                     Integer pageSize) {
        UserInfoDTO loginUser = getLoginUser();
        validatePublicKnowledgeBasePageQuery(kbType);
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<RagKnowledgeBasePO> page = ragKnowledgeBaseRepository.selectCollectedKnowledgeBasePage(
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                loginUser.getUserId(),
                keyword,
                kbType
        );
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream().map(this::toKnowledgeBaseVO).toList());
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
    public boolean isKnowledgeBaseCollected(Long kbId) {
        UserInfoDTO loginUser = getLoginUser();
        RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectPublicKnowledgeBaseById(kbId);
        if (knowledgeBase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        if (knowledgeBase.getUserId() != null && knowledgeBase.getUserId().equals(loginUser.getUserId())) {
            return false;
        }
        return ragKbUserCollectionRepository.existsActiveCollection(loginUser.getUserId(), kbId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectKnowledgeBase(Long kbId) {
        UserInfoDTO loginUser = getLoginUser();
        validatePublicKnowledgeBaseForCollection(kbId, loginUser.getUserId(), true);

        RagKbUserCollectionPO collection = ragKbUserCollectionRepository.selectCollection(loginUser.getUserId(), kbId);
        if (collection == null) {
            RagKbUserCollectionPO newCollection = RagKbUserCollectionPO.builder()
                    .userId(loginUser.getUserId())
                    .kbId(kbId)
                    .createTime(LocalDateTime.now())
                    .deleted(0)
                    .build();
            if (ragKbUserCollectionRepository.insertCollection(newCollection) != 1) {
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "收藏失败");
            }
            return;
        }

        if (collection.getDeleted() != null && collection.getDeleted() == 0) {
            return;
        }
        if (ragKbUserCollectionRepository.restoreCollection(collection.getId()) != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "收藏失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelKnowledgeBaseCollection(Long kbId) {
        UserInfoDTO loginUser = getLoginUser();
        validatePublicKnowledgeBaseForCollection(kbId, loginUser.getUserId(), false);
        ragKbUserCollectionRepository.cancelCollection(loginUser.getUserId(), kbId);
    }

    @Override
    public PageResult<RagChatSessionVO> pageChatSessions(Integer pageNum, Integer pageSize) {
        UserInfoDTO loginUser = getLoginUser();
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<RagChatSessionPO> page = ragChatSessionRepository.selectUserChatSessionPage(
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                loginUser.getUserId());
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream().map(this::toChatSessionVO).toList());
    }

    @Override
    public List<RagKnowledgeBaseVO> listChatSessionKnowledgeBases(Long sessionId) {
        UserInfoDTO loginUser = getLoginUser();
        if (sessionId == null || sessionId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "会话ID无效");
        }

        RagChatSessionPO chatSession = ragChatSessionRepository.selectUserChatSession(sessionId, loginUser.getUserId());
        if (chatSession == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "会话不存在");
        }

        return ragKnowledgeBaseRepository.selectSessionKnowledgeBases(sessionId)
                .stream()
                .map(this::toKnowledgeBaseVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RagChatSessionVO createChatSession(RagChatSessionCreateRequest request) {
        UserInfoDTO loginUser = getLoginUser();
        String sessionName = request == null ? null : request.getSessionName();
        List<Long> kbIds = request == null ? null : request.getKbIds();
        if (!StringUtils.hasText(sessionName)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "会话名称不能为空");
        }
        if (kbIds == null || kbIds.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请至少选择一个知识库");
        }

        LinkedHashSet<Long> uniqueKbIds = new LinkedHashSet<>();
        for (Long kbId : kbIds) {
            if (kbId == null || kbId <= 0) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "知识库ID无效");
            }
            uniqueKbIds.add(kbId);
        }

        List<Long> selectedKbIds = new ArrayList<>(uniqueKbIds);
        List<RagKnowledgeBasePO> knowledgeBases = new ArrayList<>(selectedKbIds.size());
        for (Long kbId : selectedKbIds) {
            RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectSelectableKnowledgeBase(loginUser.getUserId(), kbId);
            if (knowledgeBase == null) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "存在不可选的知识库");
            }
            knowledgeBases.add(knowledgeBase);
        }

        LocalDateTime now = LocalDateTime.now();
        RagChatSessionPO chatSession = RagChatSessionPO.builder()
                .userId(loginUser.getUserId())
                .sessionName(sessionName.trim())
                .kbRefCount(selectedKbIds.size())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        if (ragChatSessionRepository.insertChatSession(chatSession) != 1 || chatSession.getId() == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "会话创建失败");
        }

        for (RagKnowledgeBasePO knowledgeBase : knowledgeBases) {
            RagSessionKbRefPO sessionKbRef = RagSessionKbRefPO.builder()
                    .sessionId(chatSession.getId())
                    .kbId(knowledgeBase.getId())
                    .createTime(now)
                    .deleted(0)
                    .build();
            if (ragChatSessionRepository.insertSessionKbRef(sessionKbRef) != 1) {
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "会话创建失败");
            }
        }

        return RagChatSessionVO.builder()
                .id(chatSession.getId())
                .sessionName(chatSession.getSessionName())
                .kbRefCount(chatSession.getKbRefCount())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RagChatSessionVO renameChatSession(RagChatSessionRenameRequest request) {
        UserInfoDTO loginUser = getLoginUser();
        Long sessionId = request == null ? null : request.getSessionId();
        String sessionName = request == null ? null : request.getSessionName();
        if (sessionId == null || sessionId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "会话ID无效");
        }
        if (!StringUtils.hasText(sessionName)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "会话名称不能为空");
        }

        String trimmedSessionName = sessionName.trim();
        if (trimmedSessionName.length() > 50) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "会话名称不能超过50个字符");
        }

        RagChatSessionPO chatSession = ragChatSessionRepository.selectUserChatSession(sessionId, loginUser.getUserId());
        if (chatSession == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "会话不存在");
        }

        if (ragChatSessionRepository.renameUserChatSession(sessionId, loginUser.getUserId(), trimmedSessionName) != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "会话重命名失败");
        }

        chatSession.setSessionName(trimmedSessionName);
        return toChatSessionVO(chatSession);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatSession(Long sessionId) {
        UserInfoDTO loginUser = getLoginUser();
        validateChatSession(sessionId, loginUser.getUserId());

        List<Long> messageIds = ragChatMessageRepository.selectSessionMessageIds(sessionId);
        if (!messageIds.isEmpty()) {
            ragMsgDocRefRepository.logicalDeleteMsgDocRefs(messageIds);
            ragChatMessageRepository.logicalDeleteSessionMessages(sessionId);
        }
        ragChatSessionRepository.logicalDeleteSessionKbRefs(sessionId);

        if (ragChatSessionRepository.logicalDeleteUserChatSession(sessionId, loginUser.getUserId()) != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "内部服务器错误！");
        }
    }

    @Override
    public List<RagChatMessageVO> listChatMessages(Long sessionId) {
        UserInfoDTO loginUser = getLoginUser();
        validateChatSession(sessionId, loginUser.getUserId());
        List<RagChatMessagePO> messages = ragChatMessageRepository.selectSessionMessages(sessionId);
        return buildChatMessageVOs(messages);
    }

    @Override
    public Flux<ServerSentEvent<RagChatMessageVO>> chat(RagChatRequest request) {
        UserInfoDTO loginUser = getLoginUser();
        Long sessionId = request == null ? null : request.getSessionId();
        String message = request == null ? null : request.getMessage();
        String rewriteMessageId = request == null ? null : request.getRewriteMessageId();
        validateChatRequest(sessionId, message, loginUser.getUserId());

        if (StringUtils.hasText(rewriteMessageId)) {
            return streamRewriteChat(loginUser.getUserId(), sessionId, message.trim(), rewriteMessageId.trim());
        }
        return streamChat(loginUser.getUserId(), sessionId, message.trim(), UUID.randomUUID().toString());
    }

    private Flux<ServerSentEvent<RagChatMessageVO>> streamRewriteChat(Long userId, Long sessionId, String message,
                                                                      String rewriteMessageId) {
        try {
            List<RagChatMessagePO> messages = ragChatMessageRepository.selectSessionMessages(sessionId);
            RewriteSelection selection = resolveRewriteSelection(messages, rewriteMessageId);
            String baseMessageId = UUID.randomUUID().toString();
            List<RagChatMessagePO> history = trimHistoryMessages(messages.subList(0, selection.targetIndex()));
            RagChatContext context = buildChatContext(userId, sessionId, message, history);
            StringBuilder answer = new StringBuilder();
            OpenAiChatModel chatModel = buildOpenAiChatModel(getChatProvider());
            Flux<ServerSentEvent<RagChatMessageVO>> streamFrames = chatModel.stream(new Prompt(context.prompt()))
                    .flatMap(response -> {
                        String chunk = extractChatText(response);
                        if (!StringUtils.hasText(chunk)) {
                            return Flux.empty();
                        }
                        answer.append(chunk);
                        return Flux.just(ServerSentEvent.builder(buildSseFrame("stream", sessionId, baseMessageId + "-assistant",
                                "assistant", chunk, null, context.docRefs().size(), context.docRefs(), null)).build());
                    });

            Mono<ServerSentEvent<RagChatMessageVO>> doneFrame = Mono.fromCallable(() -> {
                RagChatMessageVO frame = new TransactionTemplate(transactionManager).execute(status ->
                        rewriteChatMessages(selection, baseMessageId, message, answer.toString(), context.docRefs()));
                if (frame == null) {
                    throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "娑堟伅淇濆瓨澶辫触");
                }
                frame.setStatus("done");
                return ServerSentEvent.builder(frame).build();
            });

            return Flux.concat(streamFrames, doneFrame)
                    .onErrorResume(ex -> Flux.just(ServerSentEvent.builder(buildSseFrame("error", sessionId,
                            baseMessageId + "-assistant", "assistant", ex instanceof BaseException ? ex.getMessage() : "AI鍥炵瓟鐢熸垚澶辫触",
                            null, 0, List.of(), null)).build()));
        } catch (Exception ex) {
            return Flux.just(ServerSentEvent.builder(buildSseFrame("error", sessionId, UUID.randomUUID().toString() + "-assistant", "assistant",
                    ex instanceof BaseException ? ex.getMessage() : "AI鍥炵瓟鐢熸垚澶辫触", null, 0, List.of(), null)).build());
        }
    }

    private Flux<ServerSentEvent<RagChatMessageVO>> streamChat(Long userId, Long sessionId, String message, String baseMessageId) {
        String assistantMessageId = baseMessageId + "-assistant";
        try {
            RagChatContext context = buildChatContext(userId, sessionId, message);
            StringBuilder answer = new StringBuilder();
            OpenAiChatModel chatModel = buildOpenAiChatModel(getChatProvider());
            Flux<ServerSentEvent<RagChatMessageVO>> streamFrames = chatModel.stream(new Prompt(context.prompt()))
                    .flatMap(response -> {
                        String chunk = extractChatText(response);
                        if (!StringUtils.hasText(chunk)) {
                            return Flux.empty();
                        }
                        answer.append(chunk);
                        return Flux.just(ServerSentEvent.builder(buildSseFrame("stream", sessionId, assistantMessageId,
                                "assistant", chunk, null, context.docRefs().size(), context.docRefs(), null)).build());
                    });

            Mono<ServerSentEvent<RagChatMessageVO>> doneFrame = Mono.fromCallable(() -> {
                RagChatMessageVO frame = new TransactionTemplate(transactionManager).execute(status ->
                        saveChatMessages(sessionId, baseMessageId, message, answer.toString(), context.docRefs()));
                if (frame == null) {
                    throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "消息保存失败");
                }
                frame.setStatus("done");
                return ServerSentEvent.builder(frame).build();
            });

            return Flux.concat(streamFrames, doneFrame)
                    .onErrorResume(ex -> Flux.just(ServerSentEvent.builder(buildSseFrame("error", sessionId,
                            assistantMessageId, "assistant", ex instanceof BaseException ? ex.getMessage() : "AI回答生成失败",
                            null, 0, List.of(), null)).build()));
        } catch (Exception ex) {
            return Flux.just(ServerSentEvent.builder(buildSseFrame("error", sessionId, assistantMessageId, "assistant",
                    ex instanceof BaseException ? ex.getMessage() : "AI回答生成失败", null, 0, List.of(), null)).build());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public RagChatMessageVO saveChatMessages(Long sessionId, String baseMessageId, String question, String answer,
                                             List<RagChatDocRefVO> docRefs) {
        LocalDateTime now = LocalDateTime.now();
        RagChatMessagePO userMessage = RagChatMessagePO.builder()
                .sessionId(sessionId)
                .messageId(baseMessageId + "-user")
                .role("user")
                .content(question)
                .metadata(null)
                .docRefCount(0)
                .createTime(now)
                .deleted(0)
                .build();
        if (ragChatMessageRepository.insertMessage(userMessage) != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "消息保存失败");
        }

        RagChatMessagePO assistantMessage = RagChatMessagePO.builder()
                .sessionId(sessionId)
                .messageId(baseMessageId + "-assistant")
                .role("assistant")
                .content(answer)
                .metadata(toDocRefInfoMetadata(docRefs))
                .docRefCount(docRefs.size())
                .createTime(now)
                .deleted(0)
                .build();
        if (ragChatMessageRepository.insertMessage(assistantMessage) != 1 || assistantMessage.getId() == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "消息保存失败");
        }

        for (RagChatDocRefVO docRef : docRefs) {
            Long docId = docRef.getDocId();
            if (docId == null) {
                continue;
            }
            RagMsgDocRefPO msgDocRef = RagMsgDocRefPO.builder()
                    .msgId(assistantMessage.getId())
                    .docId(docId)
                    .createTime(now)
                    .deleted(0)
                    .build();
            if (ragMsgDocRefRepository.insertMsgDocRef(msgDocRef) != 1) {
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "引用文档保存失败");
            }
        }

        return toChatMessageVO(assistantMessage, docRefs);
    }

    private void validateChatRequest(Long sessionId, String message, Long userId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "会话ID无效");
        }
        if (!StringUtils.hasText(message)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "消息不能为空");
        }
        validateChatSession(sessionId, userId);
    }

    private void validateChatSession(Long sessionId, Long userId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "会话ID无效");
        }
        RagChatSessionPO chatSession = ragChatSessionRepository.selectUserChatSession(sessionId, userId);
        if (chatSession == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "会话不存在");
        }
    }

    private List<RagChatMessageVO> buildChatMessageVOs(List<RagChatMessagePO> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        Long sessionId = messages.get(0).getSessionId();
        Map<Long, List<RagChatDocRefVO>> docRefsByMessageId = buildChatDocRefsByMessageId(sessionId, messages);
        return messages.stream()
                .map(message -> toChatMessageVO(message,
                        docRefsByMessageId.getOrDefault(message.getId(), parseDocRefs(message.getMetadata()))))
                .toList();
    }

    private RagChatContext buildChatContext(Long userId, Long sessionId, String message) {
        return buildChatContext(userId, sessionId, message,
                ragChatMessageRepository.selectLatestSessionMessages(sessionId, maxHistoryMessageCount()));
    }

    private RagChatContext buildChatContext(Long userId, Long sessionId, String message,
                                            List<RagChatMessagePO> historyMessages) {
        List<RagKnowledgeBasePO> knowledgeBases = ragKnowledgeBaseRepository.selectSessionKnowledgeBases(sessionId)
                .stream()
                .filter(kb -> kb.getStatus() != null && kb.getStatus() == 1)
                .filter(kb -> kb.getUserId() != null && kb.getUserId().equals(userId) || kb.getPublicFlag() != null && kb.getPublicFlag() == 1)
                .toList();
        if (knowledgeBases.isEmpty()) {
            return new RagChatContext(buildPrompt(message, List.of(), List.of()), List.of());
        }

        List<Long> kbIds = knowledgeBases.stream().map(RagKnowledgeBasePO::getId).toList();
        List<RagRepository.RagSearchChunk> chunks = ragRepository.searchVectorChunks(textEmbeddingUtil.embed(message).getVector(), kbIds);
        List<RagChatDocRefVO> docRefs = buildDocRefs(chunks, knowledgeBases);

        List<RagChatMessagePO> history = historyMessages == null ? new ArrayList<>() : new ArrayList<>(historyMessages);
        history.sort(Comparator.comparing(RagChatMessagePO::getCreateTime).thenComparing(RagChatMessagePO::getId));
        return new RagChatContext(buildPrompt(message, history, chunks), docRefs);
    }

    private List<RagChatMessagePO> trimHistoryMessages(List<RagChatMessagePO> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        int maxCount = maxHistoryMessageCount();
        if (messages.size() <= maxCount) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(messages.size() - maxCount, messages.size()));
    }

    private List<RagChatDocRefVO> buildDocRefs(List<RagRepository.RagSearchChunk> chunks, List<RagKnowledgeBasePO> knowledgeBases) {
        Map<Long, RagKnowledgeBasePO> kbMap = new HashMap<>();
        knowledgeBases.forEach(kb -> kbMap.put(kb.getId(), kb));

        LinkedHashMap<Long, RagRepository.RagSearchChunk> chunkMap = new LinkedHashMap<>();
        for (RagRepository.RagSearchChunk chunk : chunks) {
            chunkMap.putIfAbsent(chunk.docId(), chunk);
        }
        List<RagDocumentPO> documents = ragDocumentRepository.selectDocumentsByIds(new ArrayList<>(chunkMap.keySet()));
        Map<Long, RagDocumentPO> docMap = new HashMap<>();
        documents.forEach(document -> docMap.put(document.getId(), document));

        List<RagChatDocRefVO> refs = new ArrayList<>();
        for (Map.Entry<Long, RagRepository.RagSearchChunk> entry : chunkMap.entrySet()) {
            RagDocumentPO document = docMap.get(entry.getKey());
            RagKnowledgeBasePO knowledgeBase = kbMap.get(entry.getValue().kbId());
            if (document == null || knowledgeBase == null) {
                continue;
            }
            refs.add(RagChatDocRefVO.builder()
                    .docId(document.getId())
                    .kbName(knowledgeBase.getKbName())
                    .docName(document.getDocName())
                    .contentSource(entry.getValue().sourceInfo())
                    .fileUrl(document.getFileUrl())
                    .description(document.getDescription())
                    .build());
        }
        return refs;
    }

    private String buildPrompt(String question, List<RagChatMessagePO> history, List<RagRepository.RagSearchChunk> chunks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是XP-Edu平台的知识库问答助手，请基于历史对话和检索依据回答用户问题。\n");
        prompt.append("如果检索依据不足，请说明依据不足，不要编造。\n\n");
        if (history != null && !history.isEmpty()) {
            prompt.append("历史对话：\n");
            for (RagChatMessagePO item : history) {
                prompt.append(item.getRole()).append(": ").append(item.getContent()).append("\n");
            }
            prompt.append("\n");
        }
        if (chunks != null && !chunks.isEmpty()) {
            prompt.append("检索依据：\n");
            for (int i = 0; i < chunks.size(); i++) {
                RagRepository.RagSearchChunk chunk = chunks.get(i);
                prompt.append(i + 1).append(". ").append(chunk.content()).append("\n");
            }
            prompt.append("\n");
        }
        prompt.append("当前用户问题：").append(question);
        return prompt.toString();
    }

    private int maxHistoryMessageCount() {
        AIModelProperties.Provider provider = aiModelProperties.getOpenai();
        Integer count = provider == null || provider.getChatModel() == null
                ? null
                : provider.getChatModel().getMaxHistoryMessageCount();
        return count == null || count < 1 ? 10 : count;
    }

    private RewriteSelection resolveRewriteSelection(List<RagChatMessagePO> messages, String rewriteMessageId) {
        if (messages == null || messages.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅能修改最新一条提问消息");
        }

        int targetIndex = -1;
        for (int i = 0; i < messages.size(); i++) {
            RagChatMessagePO message = messages.get(i);
            if (message != null && rewriteMessageId.equals(message.getMessageId())) {
                targetIndex = i;
                if (!"user".equals(message.getRole())) {
                    throw new BaseException(HttpStatus.BAD_REQUEST, "仅能修改最新一条提问消息");
                }
                break;
            }
        }
        if (targetIndex < 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "消息不存在");
        }

        int latestUserIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            RagChatMessagePO message = messages.get(i);
            if (message != null && "user".equals(message.getRole())) {
                latestUserIndex = i;
                break;
            }
        }
        if (targetIndex != latestUserIndex) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅能修改最新一条提问消息");
        }

        return new RewriteSelection(messages.get(targetIndex), targetIndex, new ArrayList<>(messages.subList(targetIndex + 1, messages.size())));
    }

    @Transactional(rollbackFor = Exception.class)
    public RagChatMessageVO rewriteChatMessages(RewriteSelection selection, String baseMessageId, String question,
                                                String answer, List<RagChatDocRefVO> docRefs) {
        LocalDateTime now = LocalDateTime.now();
        List<Long> tailMessageIds = selection.tailMessages().stream()
                .map(RagChatMessagePO::getId)
                .filter(id -> id != null)
                .toList();
        if (!tailMessageIds.isEmpty()) {
            ragMsgDocRefRepository.logicalDeleteMsgDocRefs(tailMessageIds);
            ragChatMessageRepository.logicalDeleteMessagesByIds(tailMessageIds);
        }

        RagChatMessagePO userMessage = selection.targetMessage();
        userMessage.setMessageId(baseMessageId + "-user");
        userMessage.setContent(question);
        userMessage.setMetadata(null);
        userMessage.setDocRefCount(0);
        userMessage.setCreateTime(now);
        userMessage.setDeleted(0);
        if (ragChatMessageRepository.updateMessage(userMessage) != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "消息保存失败");
        }

        RagChatMessagePO assistantMessage = RagChatMessagePO.builder()
                .sessionId(userMessage.getSessionId())
                .messageId(baseMessageId + "-assistant")
                .role("assistant")
                .content(answer)
                .metadata(toDocRefInfoMetadata(docRefs))
                .docRefCount(docRefs.size())
                .createTime(now)
                .deleted(0)
                .build();
        if (ragChatMessageRepository.insertMessage(assistantMessage) != 1 || assistantMessage.getId() == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "消息保存失败");
        }

        for (RagChatDocRefVO docRef : docRefs) {
            Long docId = docRef.getDocId();
            if (docId == null) {
                continue;
            }
            RagMsgDocRefPO msgDocRef = RagMsgDocRefPO.builder()
                    .msgId(assistantMessage.getId())
                    .docId(docId)
                    .createTime(now)
                    .deleted(0)
                    .build();
            if (ragMsgDocRefRepository.insertMsgDocRef(msgDocRef) != 1) {
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "引用文档保存失败");
            }
        }

        return toChatMessageVO(assistantMessage, docRefs);
    }

    private AIModelProperties.Provider getChatProvider() {
        AIModelProperties.Provider provider = aiModelProperties.getOpenai();
        if (provider == null || !StringUtils.hasText(provider.getApiKey()) || !StringUtils.hasText(provider.getBaseUrl())
                || provider.getChatModel() == null || !StringUtils.hasText(provider.getChatModel().getModelName())) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "AI配置不完整，请检查 edu.ai-model.openai 相关配置");
        }
        return provider;
    }

    private String extractChatText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return response.getResult().getOutput().getText();
    }

    private RagChatMessageVO buildSseFrame(String status, Long sessionId, String messageId, String role, String content,
                                           String metadata, Integer docRefCount, List<RagChatDocRefVO> docRefs,
                                           String createTime) {
        return RagChatMessageVO.builder()
                .status(status)
                .sessionId(sessionId)
                .messageId(messageId)
                .role(role)
                .content(content)
                .metadata(metadata)
                .docRefCount(docRefCount)
                .docRefInfo(docRefs)
                .createTime(createTime)
                .build();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON序列化失败");
        }
    }

    private String toDocRefInfoMetadata(List<RagChatDocRefVO> docRefs) {
        return toJson(Map.of("docRefInfo", toJson(docRefs)));
    }

    private List<RagChatDocRefVO> parseDocRefs(String metadata) {
        if (!StringUtils.hasText(metadata)) {
            return List.of();
        }
        try {
            Map<String, String> metadataMap = objectMapper.readValue(metadata, new TypeReference<Map<String, String>>() {
            });
            String docRefInfo = metadataMap.get("docRefInfo");
            if (!StringUtils.hasText(docRefInfo)) {
                return List.of();
            }
            List<RagChatDocRefVO> refs = objectMapper.readValue(docRefInfo, new TypeReference<List<RagChatDocRefVO>>() {
            });
            return refs == null ? List.of() : refs;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private RagChatMessageVO toChatMessageVO(RagChatMessagePO message, List<RagChatDocRefVO> docRefs) {
        return RagChatMessageVO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .messageId(message.getMessageId())
                .role(message.getRole())
                .content(message.getContent())
                .metadata(message.getMetadata())
                .docRefCount(message.getDocRefCount())
                .docRefInfo(docRefs)
                .createTime(formatDateTime(message.getCreateTime()))
                .build();
    }

    private Map<Long, List<RagChatDocRefVO>> buildChatDocRefsByMessageId(Long sessionId, List<RagChatMessagePO> messages) {
        if (sessionId == null || messages == null || messages.isEmpty()) {
            return Map.of();
        }

        List<Long> messageIds = messages.stream()
                .map(RagChatMessagePO::getId)
                .filter(id -> id != null)
                .toList();
        if (messageIds.isEmpty()) {
            return Map.of();
        }

        List<RagMsgDocRefPO> msgDocRefs = ragMsgDocRefRepository.selectMsgDocRefs(messageIds);
        if (msgDocRefs.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<RagMsgDocRefPO>> msgDocRefMap = new LinkedHashMap<>();
        Set<Long> docIds = new LinkedHashSet<>();
        for (RagMsgDocRefPO msgDocRef : msgDocRefs) {
            if (msgDocRef == null || msgDocRef.getMsgId() == null) {
                continue;
            }
            msgDocRefMap.computeIfAbsent(msgDocRef.getMsgId(), key -> new ArrayList<>()).add(msgDocRef);
            if (msgDocRef.getDocId() != null) {
                docIds.add(msgDocRef.getDocId());
            }
        }

        Map<Long, RagDocumentPO> docMap = new HashMap<>();
        if (!docIds.isEmpty()) {
            ragDocumentRepository.selectDocumentsByIds(new ArrayList<>(docIds))
                    .forEach(document -> docMap.put(document.getId(), document));
        }

        Map<Long, RagKnowledgeBasePO> kbMap = new HashMap<>();
        ragKnowledgeBaseRepository.selectSessionKnowledgeBases(sessionId)
                .forEach(kb -> kbMap.put(kb.getId(), kb));

        Map<Long, List<RagChatDocRefVO>> docRefsByMessageId = new HashMap<>();
        for (RagChatMessagePO message : messages) {
            if (message == null || message.getId() == null) {
                continue;
            }
            docRefsByMessageId.put(message.getId(),
                    mergeDocRefs(parseDocRefs(message.getMetadata()), msgDocRefMap.get(message.getId()), docMap, kbMap));
        }
        return docRefsByMessageId;
    }

    private List<RagChatDocRefVO> mergeDocRefs(List<RagChatDocRefVO> parsedDocRefs, List<RagMsgDocRefPO> msgDocRefs,
                                               Map<Long, RagDocumentPO> docMap, Map<Long, RagKnowledgeBasePO> kbMap) {
        int size = Math.max(parsedDocRefs == null ? 0 : parsedDocRefs.size(), msgDocRefs == null ? 0 : msgDocRefs.size());
        if (size <= 0) {
            return List.of();
        }

        List<RagChatDocRefVO> docRefs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            RagChatDocRefVO parsedDocRef = parsedDocRefs != null && i < parsedDocRefs.size() ? parsedDocRefs.get(i) : null;
            RagMsgDocRefPO msgDocRef = msgDocRefs != null && i < msgDocRefs.size() ? msgDocRefs.get(i) : null;
            RagDocumentPO document = msgDocRef == null ? null : docMap.get(msgDocRef.getDocId());
            RagKnowledgeBasePO knowledgeBase = document == null ? null : kbMap.get(document.getKbId());

            docRefs.add(RagChatDocRefVO.builder()
                    .docId(document == null ? null : document.getId())
                    .kbName(parsedDocRef != null && StringUtils.hasText(parsedDocRef.getKbName())
                            ? parsedDocRef.getKbName()
                            : knowledgeBase == null ? null : knowledgeBase.getKbName())
                    .docName(parsedDocRef != null && StringUtils.hasText(parsedDocRef.getDocName())
                            ? parsedDocRef.getDocName()
                            : document == null ? null : document.getDocName())
                    .contentSource(parsedDocRef == null ? null : parsedDocRef.getContentSource())
                    .fileUrl(parsedDocRef != null && StringUtils.hasText(parsedDocRef.getFileUrl())
                            ? parsedDocRef.getFileUrl()
                            : document == null ? null : document.getFileUrl())
                    .description(document == null ? null : document.getDescription())
                    .build());
        }
        return docRefs;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    private record RagChatContext(String prompt, List<RagChatDocRefVO> docRefs) {
    }

    private record RewriteSelection(RagChatMessagePO targetMessage, int targetIndex, List<RagChatMessagePO> tailMessages) {
    }

    @Override
    public PageResult<RagDocumentVO> pageKnowledgeBaseDocuments(Long kbId, Integer pageNum, Integer pageSize,
                                                                String docType, String docName) {
        UserInfoDTO loginUser = getLoginUser();
        validateKnowledgeBaseId(kbId);

        RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectKnowledgeBaseById(kbId, loginUser.getUserId());
        if (knowledgeBase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "知识库不存在");
        }

        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<RagDocumentPO> page = ragDocumentRepository.selectKnowledgeBaseDocumentPage(
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                kbId,
                docType,
                docName
        );

        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream().map(this::toDocumentVO).toList());
    }

    @Override
    public List<RagDocumentVO> listPublicKnowledgeBaseDocuments(Long kbId) {
        validateKnowledgeBaseId(kbId);

        RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectPublicKnowledgeBaseById(kbId);
        if (knowledgeBase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "知识库不存在");
        }

        return ragDocumentRepository.selectKnowledgeBaseDocuments(kbId).stream().map(this::toDocumentVO).toList();
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
    public void updateRagDocument(Long kbId, Long docId, String docName, String description) {
        UserInfoDTO loginUser = getLoginUser();
        validateKnowledgeBaseId(kbId);
        validateDocumentUpdate(docId, docName);

        RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectKnowledgeBaseById(kbId, loginUser.getUserId());
        if (knowledgeBase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "知识库不存在");
        }

        RagDocumentPO document = ragDocumentRepository.selectKnowledgeBaseDocumentById(kbId, docId);
        if (document == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        if (!getDocumentType(docName).equals(document.getDocType())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "不允许修改文件后缀");
        }

        int updated = ragDocumentRepository.updateKnowledgeBaseDocument(
                kbId, docId, docName.trim(), StringUtils.hasText(description) ? description.trim() : null);
        if (updated != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "文件信息更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRagDocument(Long kbId, Long docId) {
        UserInfoDTO loginUser = getLoginUser();
        validateKnowledgeBaseId(kbId);
        validateDocumentId(docId);

        RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectKnowledgeBaseById(kbId, loginUser.getUserId());
        if (knowledgeBase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "知识库不存在");
        }

        RagDocumentPO document = ragDocumentRepository.selectKnowledgeBaseDocumentById(kbId, docId);
        if (document == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }

        int updated = ragDocumentRepository.logicalDeleteKnowledgeBaseDocument(kbId, docId);
        if (updated != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "文件删除失败");
        }
        ragRepository.logicalDeleteVectorChunks(kbId, docId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRagFile(HttpServletRequest request, MultipartFile file, String description, Long kbId) {
        validateUploadRequest(request, kbId, file);
        UserInfoDTO loginUser = getLoginUser();
        validateKnowledgeBaseForUpload(kbId, loginUser.getUserId());
        String extractFlagKey = RAG_FILE_UPLOAD_EXTRACT_FLAG_PREFIX + loginUser.getUserId();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                extractFlagKey, "1", RAG_FILE_UPLOAD_EXTRACT_FLAG_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BaseException(HttpStatus.CONFLICT, "请等待上次文件解析结束！");
        }

        List<RagTextChunkDTO> textChunks = extractText(file);
        String objectName = buildObjectName(loginUser.getUserId(), file);

        uploadFile(file, objectName);

        RagDocumentPO document = buildDocument(kbId, file, description, objectName);
        try {
            if (ragDocumentRepository.insertDocument(document) != 1 || document.getId() == null) {
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "RAG文档保存失败");
            }

            ragRepository.insertVectorChunks(buildVectorChunks(textChunks, kbId, document.getId()));
            stringRedisTemplate.delete(extractFlagKey);
        } catch (RuntimeException ex) {
            RuntimeException rollbackEx = rollbackUploadedRag(document, kbId, objectName);
            if (rollbackEx != null) {
                ex.addSuppressed(rollbackEx);
            }
            throw ex;
        }
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

    private void validateKnowledgeBaseForUpload(Long kbId, Long userId) {
        RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectKnowledgeBaseById(kbId, userId);
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

    private void validatePublicKnowledgeBaseForCollection(Long kbId, Long userId, boolean rejectOwner) {
        validateKnowledgeBaseId(kbId);
        RagKnowledgeBasePO knowledgeBase = ragKnowledgeBaseRepository.selectPublicKnowledgeBaseById(kbId);
        if (knowledgeBase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "知识库不存在");
        }
        if (rejectOwner && knowledgeBase.getUserId() != null && knowledgeBase.getUserId().equals(userId)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "该知识库由您自己创建，无需收藏");
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

    private void validatePublicKnowledgeBaseQuery(Integer kbType, Integer limit) {
        if (kbType == null || kbType < 1 || kbType > 4) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库类型无效");
        }
        if (limit == null || limit < 1 || limit > 12) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "查询数量无效");
        }
    }

    private void validatePublicKnowledgeBasePageQuery(Integer kbType) {
        if (kbType != null && (kbType < 1 || kbType > 4)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "知识库类型无效");
        }
    }

    private void validateDocumentUpdate(Long docId, String docName) {
        validateDocumentId(docId);
        if (!StringUtils.hasText(docName)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件名称不能为空");
        }
        String trimmedDocName = docName.trim();
        String fileName = StringUtils.getFilename(trimmedDocName);
        if (!trimmedDocName.equals(fileName)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件名称不合法");
        }
        if (trimmedDocName.length() > 200) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件名称最多200个字符");
        }
        getDocumentType(trimmedDocName);
    }

    private void validateDocumentId(Long docId) {
        if (docId == null || docId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件ID无效");
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
                .userId(knowledgeBase.getUserId())
                .kbName(knowledgeBase.getKbName())
                .kbCover(knowledgeBase.getKbCover())
                .description(knowledgeBase.getDescription())
                .kbType(knowledgeBase.getKbType())
                .publicFlag(knowledgeBase.getPublicFlag())
                .status(knowledgeBase.getStatus())
                .build();
    }

    private RagDocumentVO toDocumentVO(RagDocumentPO document) {
        return RagDocumentVO.builder()
                .id(document.getId())
                .kbId(document.getKbId())
                .docName(document.getDocName())
                .docType(document.getDocType())
                .description(document.getDescription())
                .fileUrl(document.getFileUrl())
                .createTime(document.getCreateTime())
                .updateTime(document.getUpdateTime())
                .build();
    }

    private RagChatSessionVO toChatSessionVO(RagChatSessionPO chatSession) {
        return RagChatSessionVO.builder()
                .id(chatSession.getId())
                .sessionName(chatSession.getSessionName())
                .kbRefCount(chatSession.getKbRefCount())
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
