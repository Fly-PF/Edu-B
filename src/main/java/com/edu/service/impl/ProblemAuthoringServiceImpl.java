package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.exception.BaseException;
import com.edu.mapper.ProblemCategoryMapper;
import com.edu.mapper.ProblemImageMapper;
import com.edu.mapper.ProblemMapper;
import com.edu.mapper.ProblemSubjectMapper;
import com.edu.mapper.ProblemVersionMapper;
import com.edu.mapper.SysUserMapper;
import com.edu.pojo.dto.problem.ProblemAuthoringRequest;
import com.edu.pojo.dto.problem.ProblemCategorySaveRequest;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.po.problem.ProblemCategoryPO;
import com.edu.pojo.po.problem.ProblemImagePO;
import com.edu.pojo.po.problem.ProblemPO;
import com.edu.pojo.po.problem.ProblemSubjectPO;
import com.edu.pojo.po.problem.ProblemVersionPO;
import com.edu.pojo.vo.problem.ProblemDetailVO;
import com.edu.pojo.vo.problem.ProblemImageVO;
import com.edu.service.ProblemAuthoringService;
import com.edu.repository.RagRepository;
import com.edu.util.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProblemAuthoringServiceImpl implements ProblemAuthoringService {
    private static final int ON_SHELF = 1;
    private static final int OFF_SHELF = 0;
    private final ProblemSubjectMapper subjectMapper;
    private final ProblemCategoryMapper categoryMapper;
    private final ProblemMapper problemMapper;
    private final ProblemImageMapper imageMapper;
    private final ProblemVersionMapper versionMapper;
    private final SysUserMapper userMapper;
    private final RagRepository ragRepository;
    private final ObjectMapper objectMapper;
    private final MinioClient minioClient;
    private final com.edu.common.properties.MinioProperties minioProperties;

    @Override
    public List<Map<String, Object>> subjects() {
        return subjectMapper.selectList(new LambdaQueryWrapper<ProblemSubjectPO>().orderByAsc(ProblemSubjectPO::getSortOrder))
                .stream().map(item -> Map.<String, Object>of("id", item.getId(), "code", item.getCode(), "name", item.getName(), "status", item.getStatus())).toList();
    }

    @Override
    public List<Map<String, Object>> categories(Long subjectId, boolean includeDisabled) {
        return categoryMapper.selectList(new LambdaQueryWrapper<ProblemCategoryPO>()
                        .eq(subjectId != null, ProblemCategoryPO::getSubjectId, subjectId)
                        .eq(!includeDisabled, ProblemCategoryPO::getStatus, ON_SHELF)
                        .orderByAsc(ProblemCategoryPO::getSortOrder))
                .stream().map(item -> Map.<String, Object>of("id", item.getId(), "subjectId", item.getSubjectId(),
                        "name", item.getName(), "description", item.getDescription() == null ? "" : item.getDescription(),
                        "sortOrder", item.getSortOrder(), "status", item.getStatus())).toList();
    }

    @Override
    @Transactional
    public Long createProblem(ProblemAuthoringRequest request, List<MultipartFile> images) {
        ProblemCategoryPO category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || category.getStatus() != ON_SHELF) throw new BaseException(HttpStatus.BAD_REQUEST, "题型分类不存在或已下架");
        ProblemPO problem = new ProblemPO();
        problem.setCategoryId(category.getId());
        problem.setTitle(request.getTitle().trim());
        problem.setIntro(request.getIntro().trim());
        problem.setDifficulty(request.getDifficulty().trim());
        problem.setTagsJson(write(request.getTags() == null ? List.of() : request.getTags().stream().filter(StringUtils::hasText).map(String::trim).toList()));
        Long userId = SecurityUtil.getLoginUser() == null ? null : SecurityUtil.getLoginUser().getUserId();
        if (userId == null) throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        problem.setCreateBy(userId);
        problem.setCurrentVersion(1);
        problem.setStatus(ON_SHELF);
        problemMapper.insert(problem);
        ProblemVersionPO version = new ProblemVersionPO();
        version.setProblemId(problem.getId());
        version.setVersion(1);
        version.setStatus(ON_SHELF);
        version.setStandardAnswer(request.getStandardAnswer().trim());
        version.setAnswerAnalysis(request.getAnswerAnalysis().trim());
        version.setDefinitionJson(write(normalizeDefinition(request.getDefinition())));
        versionMapper.insert(version);
        saveImages(problem.getId(), images);
        return problem.getId();
    }

    @Override
    @Transactional
    public void updateProblem(Long problemId, ProblemAuthoringRequest request, List<MultipartFile> images) {
        updateProblemData(problemMapper.selectById(problemId), request, images);
    }

    @Override
    @Transactional
    public void updateOwnProblem(Long problemId, ProblemAuthoringRequest request, List<MultipartFile> images) {
        updateProblemData(requireOwnProblem(problemId), request, images);
    }

    private void updateProblemData(ProblemPO problem, ProblemAuthoringRequest request, List<MultipartFile> images) {
        if (problem == null) throw new BaseException(HttpStatus.NOT_FOUND, "题目不存在");
        ProblemCategoryPO category = categoryMapper.selectById(request.getCategoryId());
        if (category == null || category.getStatus() != ON_SHELF) throw new BaseException(HttpStatus.BAD_REQUEST, "题型分类不存在或已下架");
        problem.setCategoryId(category.getId());
        problem.setTitle(request.getTitle().trim());
        problem.setIntro(request.getIntro().trim());
        problem.setDifficulty(request.getDifficulty().trim());
        problem.setTagsJson(write(request.getTags() == null ? List.of() : request.getTags().stream().filter(StringUtils::hasText).map(String::trim).toList()));
        int nextVersion = (problem.getCurrentVersion() == null ? 0 : problem.getCurrentVersion()) + 1;
        ProblemVersionPO version = new ProblemVersionPO();
        version.setProblemId(problem.getId());
        version.setVersion(nextVersion);
        version.setStatus(ON_SHELF);
        version.setStandardAnswer(request.getStandardAnswer().trim());
        version.setAnswerAnalysis(request.getAnswerAnalysis().trim());
        version.setDefinitionJson(write(normalizeDefinition(request.getDefinition())));
        versionMapper.insert(version);
        problem.setCurrentVersion(nextVersion);
        problemMapper.updateById(problem);
        if (images != null && !images.isEmpty()) {
            List<ProblemImagePO> oldImages = imageMapper.selectList(new LambdaQueryWrapper<ProblemImagePO>().eq(ProblemImagePO::getProblemId, problem.getId()));
            imageMapper.delete(new LambdaQueryWrapper<ProblemImagePO>().eq(ProblemImagePO::getProblemId, problem.getId()));
            oldImages.forEach(image -> ragRepository.deleteObject(image.getObjectKey()));
            saveImages(problem.getId(), images);
        }
    }

    @Override
    public PageResult<Map<String, Object>> problems(Integer pageNum, Integer pageSize) {
        return pageProblems(pageNum, pageSize, null);
    }

    @Override
    public PageResult<Map<String, Object>> ownProblems(Integer pageNum, Integer pageSize) {
        return pageProblems(pageNum, pageSize, requireUserId());
    }

    private PageResult<Map<String, Object>> pageProblems(Integer pageNum, Integer pageSize, Long creatorId) {
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        Page<ProblemPO> page = problemMapper.selectPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), new LambdaQueryWrapper<ProblemPO>()
                .eq(creatorId != null, ProblemPO::getCreateBy, creatorId).orderByDesc(ProblemPO::getId));
        List<ProblemPO> problems = page.getRecords();
        Map<Long, ProblemCategoryPO> categories = problems.isEmpty() ? Map.of() : categoryMapper.selectBatchIds(problems.stream().map(ProblemPO::getCategoryId).distinct().toList()).stream()
                .collect(java.util.stream.Collectors.toMap(ProblemCategoryPO::getId, item -> item));
        Map<Long, String> subjects = categories.isEmpty() ? Map.of() : subjectMapper.selectBatchIds(categories.values().stream().map(ProblemCategoryPO::getSubjectId).distinct().toList()).stream()
                .collect(java.util.stream.Collectors.toMap(ProblemSubjectPO::getId, ProblemSubjectPO::getName));
        Map<Long, String> creatorNicknames = creatorNicknames(problems);
        List<Map<String, Object>> records = problems.stream().map(item -> {
            ProblemCategoryPO category = categories.get(item.getCategoryId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId()); row.put("title", item.getTitle()); row.put("intro", item.getIntro());
            row.put("difficulty", item.getDifficulty()); row.put("status", item.getStatus()); row.put("version", item.getCurrentVersion());
            row.put("category", category == null ? "-" : category.getName());
            row.put("subject", category == null ? "-" : subjects.getOrDefault(category.getSubjectId(), "-"));
            row.put("creatorId", item.getCreateBy());
            row.put("creatorNickname", item.getCreateBy() == null ? "-" : creatorNicknames.getOrDefault(item.getCreateBy(), "-"));
            return row;
        }).toList();
        return PageResult.of(page.getTotal(), pageQuery, records);
    }

    @Override
    public PageResult<Map<String, Object>> managedCategories(Integer pageNum, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        Page<ProblemCategoryPO> page = categoryMapper.selectPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), new LambdaQueryWrapper<ProblemCategoryPO>()
                .orderByAsc(ProblemCategoryPO::getSortOrder).orderByAsc(ProblemCategoryPO::getId));
        List<Map<String, Object>> records = page.getRecords().stream().map(item -> Map.<String, Object>of(
                "id", item.getId(), "subjectId", item.getSubjectId(), "name", item.getName(),
                "description", item.getDescription() == null ? "" : item.getDescription(),
                "sortOrder", item.getSortOrder(), "status", item.getStatus())).toList();
        return PageResult.of(page.getTotal(), pageQuery, records);
    }

    @Override
    public ProblemDetailVO problem(Long problemId) {
        ProblemPO problem = problemMapper.selectById(problemId);
        if (problem == null) throw new BaseException(HttpStatus.NOT_FOUND, "题目不存在");
        ProblemCategoryPO category = categoryMapper.selectById(problem.getCategoryId());
        if (category == null) throw new BaseException(HttpStatus.NOT_FOUND, "题目分类不存在");
        ProblemSubjectPO subject = subjectMapper.selectById(category.getSubjectId());
        ProblemVersionPO version = versionMapper.selectOne(new LambdaQueryWrapper<ProblemVersionPO>()
                .eq(ProblemVersionPO::getProblemId, problemId).eq(ProblemVersionPO::getVersion, problem.getCurrentVersion()));
        if (version == null) throw new BaseException(HttpStatus.NOT_FOUND, "题目版本不存在");
        return ProblemDetailVO.builder().id(problem.getId()).title(problem.getTitle()).subjectId(category.getSubjectId()).categoryId(category.getId()).intro(problem.getIntro())
                .difficulty(problem.getDifficulty()).tags(readTags(problem.getTagsJson())).version(problem.getCurrentVersion())
                .subject(subject == null ? "-" : subject.getName()).category(category.getName())
                .creatorId(problem.getCreateBy()).creatorNickname(creatorNickname(problem.getCreateBy()))
                .standardAnswer(version.getStandardAnswer()).answerAnalysis(version.getAnswerAnalysis())
                .definition(readDefinition(version.getDefinitionJson())).images(images(problemId)).build();
    }

    @Override
    public ProblemDetailVO ownProblem(Long problemId) {
        requireOwnProblem(problemId);
        ProblemDetailVO detail = problem(problemId);
        detail.setImages(images(problemId, "/api/teacher/problem-solving/problems/"));
        return detail;
    }

    @Override
    public ResponseEntity<byte[]> getProblemImage(Long problemId, Long imageId) {
        if (problemMapper.selectById(problemId) == null) throw new BaseException(HttpStatus.NOT_FOUND, "题目不存在");
        ProblemImagePO image = imageMapper.selectOne(new LambdaQueryWrapper<ProblemImagePO>()
                .eq(ProblemImagePO::getId, imageId).eq(ProblemImagePO::getProblemId, problemId));
        if (image == null) throw new BaseException(HttpStatus.NOT_FOUND, "题图不存在");
        try (var inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName()).object(image.getObjectKey()).build())) {
            return ResponseEntity.ok().contentType(imageMediaType(image.getObjectKey()))
                    .cacheControl(CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS).cachePublic()).body(StreamUtils.copyToByteArray(inputStream));
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "题图不存在");
        }
    }

    @Override
    public void updateProblemStatus(Long problemId, Integer status) {
        checkStatus(status);
        ProblemPO problem = problemMapper.selectById(problemId);
        if (problem == null) throw new BaseException(HttpStatus.NOT_FOUND, "题目不存在");
        problem.setStatus(status);
        problemMapper.updateById(problem);
    }

    @Override
    public void updateOwnProblemStatus(Long problemId, Integer status) {
        checkStatus(status);
        ProblemPO problem = requireOwnProblem(problemId);
        problem.setStatus(status);
        problemMapper.updateById(problem);
    }

    @Override
    @Transactional
    public void deleteProblem(Long problemId) {
        ProblemPO problem = problemMapper.selectById(problemId);
        if (problem == null) throw new BaseException(HttpStatus.NOT_FOUND, "题目不存在");
        List<ProblemImagePO> images = imageMapper.selectList(new LambdaQueryWrapper<ProblemImagePO>()
                .eq(ProblemImagePO::getProblemId, problemId));
        imageMapper.delete(new LambdaQueryWrapper<ProblemImagePO>().eq(ProblemImagePO::getProblemId, problemId));
        versionMapper.delete(new LambdaQueryWrapper<ProblemVersionPO>().eq(ProblemVersionPO::getProblemId, problemId));
        problemMapper.deleteById(problemId);
        images.forEach(image -> ragRepository.deleteObject(image.getObjectKey()));
    }

    @Override
    public void deleteOwnProblem(Long problemId) {
        requireOwnProblem(problemId);
        deleteProblem(problemId);
    }

    @Override
    public Long createCategory(ProblemCategorySaveRequest request) {
        ProblemSubjectPO subject = subjectMapper.selectById(request.getSubjectId());
        if (subject == null || subject.getStatus() != ON_SHELF) throw new BaseException(HttpStatus.BAD_REQUEST, "科目不存在或已停用");
        ProblemCategoryPO category = new ProblemCategoryPO();
        category.setSubjectId(request.getSubjectId());
        category.setName(request.getName().trim()); category.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder()); category.setStatus(ON_SHELF);
        if (categoryMapper.selectCount(new LambdaQueryWrapper<ProblemCategoryPO>().eq(ProblemCategoryPO::getSubjectId, category.getSubjectId())
                .eq(ProblemCategoryPO::getName, category.getName())) > 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "该科目下已存在同名分类");
        }
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategoryStatus(Long categoryId, Integer status) {
        checkStatus(status);
        ProblemCategoryPO category = categoryMapper.selectById(categoryId);
        if (category == null) throw new BaseException(HttpStatus.NOT_FOUND, "分类不存在");
        category.setStatus(status);
        categoryMapper.updateById(category);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        ProblemCategoryPO category = categoryMapper.selectById(categoryId);
        if (category == null) throw new BaseException(HttpStatus.NOT_FOUND, "分类不存在");
        if (problemMapper.selectCount(new LambdaQueryWrapper<ProblemPO>().eq(ProblemPO::getCategoryId, categoryId)) > 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "该分类下仍有题目，不能删除");
        }
        categoryMapper.deleteById(categoryId);
    }

    private void checkStatus(Integer status) {
        if (status == null || (status != ON_SHELF && status != OFF_SHELF)) throw new BaseException(HttpStatus.BAD_REQUEST, "状态只能为上架或下架");
    }

    private void saveImages(Long problemId, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return;
        Long userId = SecurityUtil.getLoginUser() == null ? null : SecurityUtil.getLoginUser().getUserId();
        if (userId == null) throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        for (int index = 0; index < images.size(); index++) {
            MultipartFile image = images.get(index);
            String extension = imageExtension(image);
            String objectKey = "edu/exp/files/" + userId + "/" + extension + "/" + UUID.randomUUID() + "." + extension;
            ragRepository.uploadObject(image, objectKey);
            ProblemImagePO problemImage = new ProblemImagePO();
            problemImage.setProblemId(problemId);
            problemImage.setObjectKey(objectKey);
            problemImage.setOriginalFilename(originalFilename(image));
            problemImage.setSortOrder(index);
            imageMapper.insert(problemImage);
        }
    }

    private String imageExtension(MultipartFile image) {
        if (image == null || image.isEmpty() || image.getSize() > 10 * 1024 * 1024) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "图片不能为空且不能超过10MB");
        }
        String extension = StringUtils.getFilenameExtension(originalFilename(image));
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        Map<String, String> contentTypes = Map.of("jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp");
        if (!Set.copyOf(contentTypes.keySet()).contains(extension) || !contentTypes.get(extension).equals(image.getContentType())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅支持jpg、jpeg、png、webp格式图片");
        }
        return extension;
    }

    private String originalFilename(MultipartFile image) {
        String filename = StringUtils.getFilename(image == null ? null : image.getOriginalFilename());
        if (!StringUtils.hasText(filename) || filename.length() > 255) throw new BaseException(HttpStatus.BAD_REQUEST, "图片文件名不合法");
        return filename;
    }

    private Map<String, Object> normalizeDefinition(Map<String, Object> definition) {
        Map<String, Object> normalized = new LinkedHashMap<>(definition);
        normalized.put("schemaVersion", "2.0");
        normalized.put("lessonPolicy", Map.of("stepCount", 4));
        if (!(normalized.get("visualCatalog") instanceof Collection<?>)) normalized.put("visualCatalog", List.of());
        if (!(normalized.get("sceneDefinition") instanceof Map<?, ?>)) normalized.put("sceneDefinition", Map.of("sceneType", "textOnly", "variables", Map.of()));
        return normalized;
    }

    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (Exception ex) { throw new BaseException(HttpStatus.BAD_REQUEST, "题目定义格式错误"); }
    }

    private Map<String, Object> readDefinition(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception ex) { throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "题目定义格式错误"); }
    }

    private List<String> readTags(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception ex) { return List.of(); }
    }

    private ProblemPO requireOwnProblem(Long problemId) {
        ProblemPO problem = problemMapper.selectOne(new LambdaQueryWrapper<ProblemPO>()
                .eq(ProblemPO::getId, problemId)
                .eq(ProblemPO::getCreateBy, requireUserId()));
        if (problem == null) throw new BaseException(HttpStatus.NOT_FOUND, "题目不存在");
        return problem;
    }

    private Long requireUserId() {
        Long userId = SecurityUtil.getLoginUser() == null ? null : SecurityUtil.getLoginUser().getUserId();
        if (userId == null) throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        return userId;
    }

    private List<ProblemImageVO> images(Long problemId) {
        return images(problemId, "/api/admin/problem-solving/problems/");
    }

    private List<ProblemImageVO> images(Long problemId, String urlPrefix) {
        return imageMapper.selectList(new LambdaQueryWrapper<ProblemImagePO>().eq(ProblemImagePO::getProblemId, problemId)
                        .orderByAsc(ProblemImagePO::getSortOrder).orderByAsc(ProblemImagePO::getId)).stream()
                .map(item -> ProblemImageVO.builder().id(item.getId()).originalFilename(item.getOriginalFilename())
                        .url(urlPrefix + problemId + "/images/" + item.getId()).build()).toList();
    }

    private Map<Long, String> creatorNicknames(List<ProblemPO> problems) {
        List<Long> userIds = problems.stream().map(ProblemPO::getCreateBy).filter(java.util.Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(SysUserPO::getId, this::creatorNickname));
    }

    private String creatorNickname(Long userId) {
        return userId == null ? "-" : creatorNickname(userMapper.selectById(userId));
    }

    private String creatorNickname(SysUserPO user) {
        if (user == null) return "-";
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private String bucketName() {
        if (!StringUtils.hasText(minioProperties.getBuckerName())) throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO存储桶未配置");
        return minioProperties.getBuckerName();
    }

    private MediaType imageMediaType(String objectKey) {
        String extension = StringUtils.getFilenameExtension(objectKey);
        return switch (extension == null ? "" : extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
