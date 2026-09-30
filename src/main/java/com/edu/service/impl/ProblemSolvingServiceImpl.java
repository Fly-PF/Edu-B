package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.common.properties.AIModelProperties;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.mapper.ProblemCategoryMapper;
import com.edu.mapper.ProblemImageMapper;
import com.edu.mapper.ProblemMapper;
import com.edu.mapper.ProblemSubjectMapper;
import com.edu.mapper.ProblemVersionMapper;
import com.edu.pojo.dto.problem.ProblemExplainRequest;
import com.edu.pojo.dto.problem.ProblemSessionCreateRequest;
import com.edu.pojo.po.problem.ProblemCategoryPO;
import com.edu.pojo.po.problem.ProblemImagePO;
import com.edu.pojo.po.problem.ProblemPO;
import com.edu.pojo.po.problem.ProblemSubjectPO;
import com.edu.pojo.po.problem.ProblemVersionPO;
import com.edu.pojo.vo.problem.ProblemCategoryVO;
import com.edu.pojo.vo.problem.ProblemDetailVO;
import com.edu.pojo.vo.problem.ProblemExplanationVO;
import com.edu.pojo.vo.problem.ProblemListItemVO;
import com.edu.pojo.vo.problem.ProblemImageVO;
import com.edu.pojo.vo.problem.ProblemSubjectVO;
import com.edu.pojo.vo.problem.ProblemSessionVO;
import com.edu.problemsolving.session.ProblemLessonSession;
import com.edu.problemsolving.session.ProblemLessonSessionRegistry;
import com.edu.service.ProblemSolvingService;
import com.edu.util.SecurityUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemSolvingServiceImpl implements ProblemSolvingService {
    private static final int PUBLISHED = 1;

    private final ProblemSubjectMapper subjectMapper;
    private final ProblemCategoryMapper categoryMapper;
    private final ProblemMapper problemMapper;
    private final ProblemImageMapper imageMapper;
    private final ProblemVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final AIModelProperties aiModelProperties;
    private final ProblemLessonSessionRegistry lessonSessionRegistry;

    @Override
    public List<ProblemSubjectVO> listSubjects() {
        return subjectMapper.selectList(new LambdaQueryWrapper<ProblemSubjectPO>()
                        .eq(ProblemSubjectPO::getStatus, PUBLISHED)
                        .orderByAsc(ProblemSubjectPO::getSortOrder))
                .stream().map(item -> ProblemSubjectVO.builder()
                        .id(item.getId()).code(item.getCode()).name(item.getName()).build()).toList();
    }

    @Override
    public List<ProblemCategoryVO> listCategories(Long subjectId) {
        return categoryMapper.selectList(new LambdaQueryWrapper<ProblemCategoryPO>()
                        .eq(ProblemCategoryPO::getStatus, PUBLISHED)
                        .eq(subjectId != null, ProblemCategoryPO::getSubjectId, subjectId)
                        .orderByAsc(ProblemCategoryPO::getSortOrder))
                .stream().map(item -> ProblemCategoryVO.builder().id(item.getId())
                        .name(item.getName()).description(item.getDescription()).build()).toList();
    }

    @Override
    public PageResult<ProblemListItemVO> pageProblems(Long subjectId, Long categoryId, String difficulty,
                                                       String keyword, Integer pageNum, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        List<ProblemCategoryPO> categories = categoryMapper.selectList(new LambdaQueryWrapper<ProblemCategoryPO>()
                .eq(ProblemCategoryPO::getStatus, PUBLISHED)
                .eq(subjectId != null, ProblemCategoryPO::getSubjectId, subjectId)
                .eq(categoryId != null, ProblemCategoryPO::getId, categoryId));
        if (categories.isEmpty()) {
            return PageResult.of(0, pageQuery, List.of());
        }
        Map<Long, ProblemCategoryPO> categoryById = categories.stream()
                .collect(Collectors.toMap(ProblemCategoryPO::getId, Function.identity()));
        Map<Long, String> subjectById = subjectMapper.selectBatchIds(categories.stream()
                        .map(ProblemCategoryPO::getSubjectId).distinct().toList()).stream()
                .collect(Collectors.toMap(ProblemSubjectPO::getId, ProblemSubjectPO::getName));
        IPage<ProblemPO> page = problemMapper.selectPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()),
                new LambdaQueryWrapper<ProblemPO>().eq(ProblemPO::getStatus, PUBLISHED)
                        .in(ProblemPO::getCategoryId, categoryById.keySet())
                        .eq(StringUtils.hasText(difficulty), ProblemPO::getDifficulty, trim(difficulty))
                        .and(StringUtils.hasText(keyword), item -> item.like(ProblemPO::getTitle, trim(keyword))
                                .or().like(ProblemPO::getIntro, trim(keyword))
                                .or().apply("JSON_SEARCH(tags_json, 'one', {0}) IS NOT NULL", "%" + trim(keyword) + "%"))
                        .orderByDesc(ProblemPO::getId));
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream()
                .map(item -> toListItem(item, categoryById.get(item.getCategoryId()), subjectById)).toList());
    }

    @Override
    public ProblemDetailVO getProblem(Long problemId) {
        ProblemPO problem = requireProblem(problemId);
        ProblemCategoryPO category = requireCategory(problem.getCategoryId());
        ProblemSubjectPO subject = subjectMapper.selectById(category.getSubjectId());
        if (subject == null || subject.getStatus() != PUBLISHED) {
            throw notFound();
        }
        ProblemVersionPO version = requireVersion(problem);
        return ProblemDetailVO.builder().id(problem.getId()).title(problem.getTitle()).intro(problem.getIntro())
                .difficulty(problem.getDifficulty()).tags(readTags(problem.getTagsJson())).version(problem.getCurrentVersion())
                .subject(subject.getName()).category(category.getName())
                .standardAnswer(version.getStandardAnswer()).answerAnalysis(version.getAnswerAnalysis())
                .definition(readDefinition(version.getDefinitionJson())).images(images(problem.getId())).build();
    }

    @Override
    public ResponseEntity<byte[]> getProblemImage(Long problemId, Long imageId) {
        requireProblem(problemId);
        ProblemImagePO image = imageMapper.selectOne(new LambdaQueryWrapper<ProblemImagePO>()
                .eq(ProblemImagePO::getId, imageId).eq(ProblemImagePO::getProblemId, problemId));
        if (image == null) throw notFound();
        try (var inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName()).object(image.getObjectKey()).build())) {
            return ResponseEntity.ok().contentType(imageMediaType(image.getObjectKey()))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic()).body(StreamUtils.copyToByteArray(inputStream));
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "题图不存在");
        }
    }

    @Override
    public ProblemExplanationVO explain(Long problemId, ProblemExplainRequest request) {
        Map<String, Object> definition = definitionFor(problemId);
        Map<String, Object> card = maps(definition.get("cards")).stream()
                .filter(item -> request.getCardId().equals(string(item.get("id")))).findFirst()
                .orElseThrow(() -> new BaseException(HttpStatus.BAD_REQUEST, "讲题步骤不存在"));
        String level = Set.of("basic", "sprint", "perfect").contains(request.getLevel()) ? request.getLevel() : "basic";
        String fallback = string(map(card.get("explanations")).get(level));
        try {
            return ProblemExplanationVO.builder().text(ChatClient.builder(buildChatModel()).build().prompt()
                    .system("你是 AI 解题导师。仅根据给定题目事实回答，不能修改公式、答案、条件或发明新结论。用简洁中文回答，不要提及内部规则。")
                    .user("题面：" + string(definition.get("stem")) + "\n当前步骤：" + string(card.get("title"))
                            + "\n已审核讲解：" + fallback + "\n学生问题：" + request.getQuestion().trim())
                    .call().content()).fallback(false).build();
        } catch (Exception ignored) {
            return ProblemExplanationVO.builder().text(fallback).fallback(true).build();
        }
    }

    @Override
    public ProblemSessionVO createSession(ProblemSessionCreateRequest request) {
        if (SecurityUtil.getLoginUser() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        ProblemDetailVO problem = getProblem(request.getProblemId());
        ProblemLessonSession session = lessonSessionRegistry.create(SecurityUtil.getLoginUser().getUserId(), problem.getId(),
                problem.getVersion(), initialScene(problem.getDefinition()));
        return ProblemSessionVO.builder().clientSessionId(session.getClientSessionId()).wsToken(session.getWsToken())
                .problemVersion(problem.getVersion()).wsEndpoint("/ws/problem-solving").build();
    }

    private ProblemPO requireProblem(Long problemId) {
        ProblemPO problem = problemMapper.selectOne(new LambdaQueryWrapper<ProblemPO>()
                .eq(ProblemPO::getId, problemId).eq(ProblemPO::getStatus, PUBLISHED));
        if (problem == null) throw notFound();
        return problem;
    }

    private ProblemCategoryPO requireCategory(Long categoryId) {
        ProblemCategoryPO category = categoryMapper.selectById(categoryId);
        if (category == null || category.getStatus() != PUBLISHED) throw notFound();
        return category;
    }

    private ProblemVersionPO requireVersion(ProblemPO problem) {
        ProblemVersionPO version = versionMapper.selectOne(new LambdaQueryWrapper<ProblemVersionPO>()
                .eq(ProblemVersionPO::getProblemId, problem.getId()).eq(ProblemVersionPO::getVersion, problem.getCurrentVersion())
                .eq(ProblemVersionPO::getStatus, PUBLISHED));
        if (version == null) throw notFound();
        return version;
    }

    private Map<String, Object> definitionFor(Long problemId) {
        return readDefinition(requireVersion(requireProblem(problemId)).getDefinitionJson());
    }

    private ProblemListItemVO toListItem(ProblemPO problem, ProblemCategoryPO category, Map<Long, String> subjectById) {
        return ProblemListItemVO.builder().id(problem.getId()).title(problem.getTitle()).intro(problem.getIntro())
                .difficulty(problem.getDifficulty()).tags(readTags(problem.getTagsJson())).mastery("待学习")
                .version(problem.getCurrentVersion()).category(category.getName()).subject(subjectById.get(category.getSubjectId())).build();
    }

    private List<ProblemImageVO> images(Long problemId) {
        return imageMapper.selectList(new LambdaQueryWrapper<ProblemImagePO>().eq(ProblemImagePO::getProblemId, problemId)
                        .orderByAsc(ProblemImagePO::getSortOrder).orderByAsc(ProblemImagePO::getId)).stream()
                .map(item -> ProblemImageVO.builder().id(item.getId()).originalFilename(item.getOriginalFilename())
                        .url("/api/problem-solving/problems/" + problemId + "/images/" + item.getId()).build()).toList();
    }

    private String bucketName() {
        if (!StringUtils.hasText(minioProperties.getBuckerName())) throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO存储桶未配置");
        return minioProperties.getBuckerName();
    }

    private MediaType imageMediaType(String objectKey) {
        String extension = StringUtils.getFilenameExtension(objectKey);
        return switch (extension == null ? "" : extension.toLowerCase(java.util.Locale.ROOT)) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private OpenAiChatModel buildChatModel() {
        AIModelProperties.Model model = aiModelProperties.getRag().getChatModel();
        if (model == null || !StringUtils.hasText(model.getApiKey()) || !StringUtils.hasText(model.getBaseUrl())
                || !StringUtils.hasText(model.getModelName())) {
            throw new BaseException(HttpStatus.SERVICE_UNAVAILABLE, "AI配置不完整，请检查 edu.ai-model.rag.chat-model");
        }
        return OpenAiChatModel.builder().options(OpenAiChatOptions.builder().apiKey(model.getApiKey())
                        .baseUrl(model.getBaseUrl()).model(model.getModelName()).build())
                .observationRegistry(ObservationRegistry.NOOP).build();
    }

    private Map<String, Object> readDefinition(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "题目定义格式错误");
        }
    }

    private Map<String, Object> initialScene(Map<String, Object> definition) {
        Map<String, Object> sceneDefinition = map(definition.get("sceneDefinition"));
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map(sceneDefinition.get("variables")).entrySet()) {
            Map<String, Object> variable = map(entry.getValue());
            if (variable.containsKey("value")) values.put(entry.getKey(), variable.get("value"));
        }
        values.put("highlight", List.of());
        values.put("revealed", List.of());
        return values;
    }

    private List<String> readTags(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) { return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of(); }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Object value) { return value instanceof Collection<?> values ? values.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList() : List.of(); }
    private List<String> strings(Object value) { return value instanceof Collection<?> values ? values.stream().map(this::string).toList() : List.of(); }
    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private BaseException notFound() { return new BaseException(HttpStatus.NOT_FOUND, "讲题不存在或未发布"); }
}
