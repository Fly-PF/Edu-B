package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.mapper.skill.AiSkillCallLogMapper;
import com.edu.mapper.skill.AiSkillCategoryMapper;
import com.edu.mapper.skill.AiSkillCategoryRelMapper;
import com.edu.mapper.skill.AiSkillCollectionMapper;
import com.edu.mapper.skill.AiSkillMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.skill.AiSkillCategorySaveRequest;
import com.edu.pojo.dto.skill.AiSkillFileDTO;
import com.edu.pojo.dto.skill.AiSkillInvokeRequest;
import com.edu.pojo.dto.skill.AiSkillSaveRequest;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.po.skill.AiSkillCallLogPO;
import com.edu.pojo.po.skill.AiSkillCategoryPO;
import com.edu.pojo.po.skill.AiSkillCategoryRelPO;
import com.edu.pojo.po.skill.AiSkillCollectionPO;
import com.edu.pojo.po.skill.AiSkillPO;
import com.edu.pojo.vo.skill.AiSkillCategoryVO;
import com.edu.pojo.vo.skill.AiSkillFileVO;
import com.edu.pojo.vo.skill.AiSkillInvokeVO;
import com.edu.pojo.vo.skill.AiSkillVO;
import com.edu.repository.SysUserRepository;
import com.edu.service.AiSkillService;
import com.edu.util.SecurityUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSkillServiceImpl implements AiSkillService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String VISIBILITY_PRIVATE = "PRIVATE";
    private static final String VISIBILITY_PUBLIC = "PUBLIC";
    private static final String REVIEW_APPROVED = "APPROVED";
    private static final Set<String> SKILL_TYPES = Set.of("TEACHING_METHOD", "ANSWER_STEPS", "GRADING_STANDARD", "STUDY_PLAN", "OTHER");
    private static final Set<String> CATEGORY_TYPES = Set.of("SUBJECT", "AUDIENCE", "OTHER");
    private static final String ROOT_PREFIX = "edu-skill-files/";

    private final AiSkillMapper skillMapper;
    private final AiSkillCategoryMapper categoryMapper;
    private final AiSkillCategoryRelMapper categoryRelMapper;
    private final AiSkillCollectionMapper collectionMapper;
    private final AiSkillCallLogMapper callLogMapper;
    private final SysUserRepository sysUserRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public PageResult<AiSkillVO> pageMarket(String keyword, String skillType, String subjectType, String targetAudience,
                                            Long categoryId, Integer pageNum, Integer pageSize) {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        LambdaQueryWrapper<AiSkillPO> wrapper = baseVisibleMarketWrapper(keyword, skillType, subjectType, targetAudience);
        if (categoryId != null) {
            List<Long> skillIds = categoryRelMapper.selectList(new LambdaQueryWrapper<AiSkillCategoryRelPO>()
                            .eq(AiSkillCategoryRelPO::getDeleted, 0)
                            .eq(AiSkillCategoryRelPO::getCategoryId, categoryId))
                    .stream()
                    .map(AiSkillCategoryRelPO::getSkillId)
                    .distinct()
                    .toList();
            if (skillIds.isEmpty()) {
                return PageResult.of(0, pageQuery, List.of());
            }
            wrapper.in(AiSkillPO::getId, skillIds);
        }
        IPage<AiSkillPO> page = skillMapper.selectPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), wrapper);
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream()
                .map(skill -> toVO(skill, user, false))
                .toList());
    }

    @Override
    public PageResult<AiSkillVO> pageMySkills(String keyword, String status, Integer pageNum, Integer pageSize) {
        UserInfoDTO user = requireUser();
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        LambdaQueryWrapper<AiSkillPO> wrapper = new LambdaQueryWrapper<AiSkillPO>()
                .eq(AiSkillPO::getDeleted, 0)
                .eq(AiSkillPO::getUserId, user.getUserId())
                .and(StringUtils.hasText(keyword), q -> q.like(AiSkillPO::getSkillName, keyword).or().like(AiSkillPO::getDescription, keyword))
                .eq(StringUtils.hasText(status), AiSkillPO::getStatus, normalizeStatus(status))
                .orderByDesc(AiSkillPO::getUpdateTime)
                .orderByDesc(AiSkillPO::getId);
        IPage<AiSkillPO> page = skillMapper.selectPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), wrapper);
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream()
                .map(skill -> toVO(skill, user, false))
                .toList());
    }

    @Override
    public AiSkillVO detail(Long skillId) {
        UserInfoDTO user = requireUser();
        AiSkillPO skill = requireVisibleSkill(skillId, user);
        boolean includeContent = isOwner(skill, user) || isAdmin(user);
        return toVO(skill, user, includeContent);
    }

    @Override
    @Transactional
    public AiSkillVO createDraft(AiSkillSaveRequest request) {
        UserInfoDTO user = requireUser();
        SkillFileBundle bundle = validateAndNormalizeFiles(request == null ? null : request.getSkillFiles());
        validateContent(bundle);
        LocalDateTime now = LocalDateTime.now();
        String rootPath = newRootPath(user.getUserId());
        writeFiles(rootPath, bundle.files());
        AiSkillPO skill = AiSkillPO.builder()
                .userId(user.getUserId())
                .skillName(requiredText(request.getSkillName(), "Skill名称不能为空"))
                .description(trimToNull(request.getDescription()))
                .skillType(normalizeSkillType(request.getSkillType()))
                .subjectType(trimToNull(request.getSubjectType()))
                .targetAudience(trimToNull(request.getTargetAudience()))
                .usageGuide(trimToNull(request.getUsageGuide()))
                .exampleInput(trimToNull(request.getExampleInput()))
                .exampleOutput(trimToNull(request.getExampleOutput()))
                .visibility(normalizeVisibility(request.getVisibility()))
                .status(STATUS_DRAFT)
                .reviewStatus(REVIEW_APPROVED)
                .reviewMessage("格式和内容检查通过")
                .rootObjectPath(rootPath)
                .deleted(0)
                .createTime(now)
                .updateTime(now)
                .build();
        skillMapper.insert(skill);
        saveCategoryRelations(skill.getId(), request.getCategoryIds());
        return toVO(skill, user, true);
    }

    @Override
    @Transactional
    public AiSkillVO updateSkill(Long skillId, AiSkillSaveRequest request) {
        UserInfoDTO user = requireUser();
        AiSkillPO skill = requireOwnerSkill(skillId, user);
        SkillFileBundle bundle = validateAndNormalizeFiles(request == null ? null : request.getSkillFiles());
        validateContent(bundle);
        String oldRoot = skill.getRootObjectPath();
        String newRoot = newRootPath(user.getUserId());
        writeFiles(newRoot, bundle.files());
        skill.setSkillName(requiredText(request.getSkillName(), "Skill名称不能为空"));
        skill.setDescription(trimToNull(request.getDescription()));
        skill.setSkillType(normalizeSkillType(request.getSkillType()));
        skill.setSubjectType(trimToNull(request.getSubjectType()));
        skill.setTargetAudience(trimToNull(request.getTargetAudience()));
        skill.setUsageGuide(trimToNull(request.getUsageGuide()));
        skill.setExampleInput(trimToNull(request.getExampleInput()));
        skill.setExampleOutput(trimToNull(request.getExampleOutput()));
        skill.setVisibility(normalizeVisibility(request.getVisibility()));
        skill.setReviewStatus(REVIEW_APPROVED);
        skill.setReviewMessage("格式和内容检查通过");
        skill.setRootObjectPath(newRoot);
        skill.setUpdateTime(LocalDateTime.now());
        skillMapper.updateById(skill);
        saveCategoryRelations(skill.getId(), request.getCategoryIds());
        removePrefixQuietly(oldRoot);
        return toVO(skill, user, true);
    }

    @Override
    @Transactional
    public AiSkillVO publish(Long skillId) {
        UserInfoDTO user = requireUser();
        AiSkillPO skill = requireOwnerSkill(skillId, user);
        if (!STATUS_DRAFT.equals(skill.getStatus()) && !STATUS_OFFLINE.equals(skill.getStatus())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "当前状态不可发布");
        }
        if (!REVIEW_APPROVED.equals(skill.getReviewStatus())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "内容检查未通过，不能发布");
        }
        skill.setStatus(STATUS_PUBLISHED);
        skill.setUpdateTime(LocalDateTime.now());
        skillMapper.updateById(skill);
        return toVO(skill, user, false);
    }

    @Override
    @Transactional
    public AiSkillVO offline(Long skillId) {
        UserInfoDTO user = requireUser();
        AiSkillPO skill = requireOwnerSkill(skillId, user);
        if (!STATUS_PUBLISHED.equals(skill.getStatus())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅已发布Skill可以下线");
        }
        skill.setStatus(STATUS_OFFLINE);
        skill.setUpdateTime(LocalDateTime.now());
        skillMapper.updateById(skill);
        return toVO(skill, user, false);
    }

    @Override
    @Transactional
    public void deleteSkill(Long skillId) {
        UserInfoDTO user = requireUser();
        AiSkillPO skill = requireOwnerSkill(skillId, user);
        if (STATUS_DRAFT.equals(skill.getStatus())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "草稿状态不支持删除，请先发布或完善后处理");
        }
        skill.setDeleted(1);
        skill.setUpdateTime(LocalDateTime.now());
        skillMapper.updateById(skill);
        categoryRelMapper.delete(new LambdaQueryWrapper<AiSkillCategoryRelPO>().eq(AiSkillCategoryRelPO::getSkillId, skillId));
        collectionMapper.update(null, new LambdaUpdateWrapper<AiSkillCollectionPO>()
                .eq(AiSkillCollectionPO::getSkillId, skillId)
                .set(AiSkillCollectionPO::getDeleted, 1)
                .set(AiSkillCollectionPO::getUpdateTime, LocalDateTime.now()));
        removePrefixQuietly(skill.getRootObjectPath());
    }

    @Override
    @Transactional
    public void collect(Long skillId) {
        UserInfoDTO user = requireUser();
        AiSkillPO skill = requirePublicPublishedSkill(skillId);
        AiSkillCollectionPO collection = collectionMapper.selectOne(new LambdaQueryWrapper<AiSkillCollectionPO>()
                .eq(AiSkillCollectionPO::getUserId, user.getUserId())
                .eq(AiSkillCollectionPO::getSkillId, skill.getId())
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (collection == null) {
            collectionMapper.insert(AiSkillCollectionPO.builder()
                    .userId(user.getUserId())
                    .skillId(skill.getId())
                    .deleted(0)
                    .createTime(now)
                    .updateTime(now)
                    .build());
        } else if (!Objects.equals(collection.getDeleted(), 0)) {
            collection.setDeleted(0);
            collection.setUpdateTime(now);
            collectionMapper.updateById(collection);
        }
    }

    @Override
    @Transactional
    public void cancelCollect(Long skillId) {
        UserInfoDTO user = requireUser();
        collectionMapper.update(null, new LambdaUpdateWrapper<AiSkillCollectionPO>()
                .eq(AiSkillCollectionPO::getUserId, user.getUserId())
                .eq(AiSkillCollectionPO::getSkillId, skillId)
                .set(AiSkillCollectionPO::getDeleted, 1)
                .set(AiSkillCollectionPO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public PageResult<AiSkillVO> pageCollections(Integer pageNum, Integer pageSize) {
        UserInfoDTO user = requireUser();
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<AiSkillCollectionPO> page = collectionMapper.selectPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()),
                new LambdaQueryWrapper<AiSkillCollectionPO>()
                        .eq(AiSkillCollectionPO::getUserId, user.getUserId())
                        .eq(AiSkillCollectionPO::getDeleted, 0)
                        .orderByDesc(AiSkillCollectionPO::getUpdateTime));
        List<Long> skillIds = page.getRecords().stream().map(AiSkillCollectionPO::getSkillId).toList();
        if (skillIds.isEmpty()) {
            return PageResult.of(page.getTotal(), pageQuery, List.of());
        }
        Map<Long, AiSkillPO> skillMap = new LinkedHashMap<>();
        skillMapper.selectList(baseVisibleMarketWrapper(null, null, null, null).in(AiSkillPO::getId, skillIds))
                .forEach(skill -> skillMap.put(skill.getId(), skill));
        return PageResult.of(page.getTotal(), pageQuery, skillIds.stream()
                .map(skillMap::get)
                .filter(Objects::nonNull)
                .map(skill -> toVO(skill, user, false))
                .toList());
    }

    @Override
    @Transactional
    public AiSkillVO importZip(MultipartFile file, String skillName, String description, String skillType,
                               String subjectType, String targetAudience, String visibility, List<Long> categoryIds) {
        UserInfoDTO user = requireUser();
        if (file == null || file.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请上传Skill zip文件");
        }
        SkillFileBundle bundle = readSkillZip(file);
        validateContent(bundle);
        AiSkillSaveRequest request = new AiSkillSaveRequest();
        request.setSkillName(StringUtils.hasText(skillName) ? skillName : inferSkillName(bundle));
        request.setDescription(description);
        request.setSkillType(skillType);
        request.setSubjectType(subjectType);
        request.setTargetAudience(targetAudience);
        request.setVisibility(StringUtils.hasText(visibility) ? visibility : VISIBILITY_PRIVATE);
        request.setCategoryIds(categoryIds == null ? List.of() : categoryIds);
        request.setSkillFiles(bundle.files());
        return createDraft(request);
    }

    @Override
    public ResponseEntity<byte[]> exportSkill(Long skillId) {
        UserInfoDTO user = requireUser();
        AiSkillPO skill = requireVisibleSkill(skillId, user);
        if (!isOwner(skill, user) && !isPublicPublished(skill)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权导出该Skill");
        }
        byte[] bytes = buildZipBytes(skill);
        String fileName = sanitizeDownloadName(skill.getSkillName()) + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @Override
    public AiSkillInvokeVO invokeSkill(Long skillId, AiSkillInvokeRequest request) {
        UserInfoDTO user = requireUser();
        AiSkillPO skill = requireVisibleSkill(skillId, user);
        String inputText = requiredText(request == null ? null : request.getInputText(), "调用输入不能为空");
        String sourceModule = trimToNull(request == null ? null : request.getSourceModule());
        List<AiSkillFileVO> files = readFiles(skill.getRootObjectPath(), true);
        String assembledPrompt = buildInvokePrompt(skill, files, inputText);
        callLogMapper.insert(AiSkillCallLogPO.builder()
                .skillId(skill.getId())
                .userId(user.getUserId())
                .sourceModule(sourceModule)
                .inputText(inputText)
                .outputText(assembledPrompt)
                .success(1)
                .createTime(LocalDateTime.now())
                .build());
        return AiSkillInvokeVO.builder()
                .skillId(skill.getId())
                .skillName(skill.getSkillName())
                .sourceModule(sourceModule)
                .inputText(inputText)
                .assembledPrompt(assembledPrompt)
                .build();
    }

    @Override
    public List<AiSkillCategoryVO> listCategories(String categoryType, Boolean enabledOnly) {
        LambdaQueryWrapper<AiSkillCategoryPO> wrapper = new LambdaQueryWrapper<AiSkillCategoryPO>()
                .eq(AiSkillCategoryPO::getDeleted, 0)
                .eq(StringUtils.hasText(categoryType), AiSkillCategoryPO::getCategoryType, normalizeCategoryType(categoryType))
                .eq(Boolean.TRUE.equals(enabledOnly), AiSkillCategoryPO::getEnabled, 1)
                .orderByAsc(AiSkillCategoryPO::getSort)
                .orderByDesc(AiSkillCategoryPO::getCreateTime);
        return categoryMapper.selectList(wrapper).stream().map(this::toCategoryVO).toList();
    }

    @Override
    @Transactional
    public AiSkillCategoryVO createCategory(AiSkillCategorySaveRequest request) {
        AiSkillCategoryPO category = AiSkillCategoryPO.builder()
                .categoryType(normalizeCategoryType(request.getCategoryType()))
                .categoryName(requiredText(request.getCategoryName(), "分类名称不能为空"))
                .sort(request.getSort() == null ? 0 : request.getSort())
                .enabled(request.getEnabled() == null ? 1 : normalizeEnabled(request.getEnabled()))
                .deleted(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        categoryMapper.insert(category);
        return toCategoryVO(category);
    }

    @Override
    @Transactional
    public AiSkillCategoryVO updateCategory(Long categoryId, AiSkillCategorySaveRequest request) {
        AiSkillCategoryPO category = requireCategory(categoryId);
        category.setCategoryType(normalizeCategoryType(request.getCategoryType()));
        category.setCategoryName(requiredText(request.getCategoryName(), "分类名称不能为空"));
        category.setSort(request.getSort() == null ? category.getSort() : request.getSort());
        category.setEnabled(request.getEnabled() == null ? category.getEnabled() : normalizeEnabled(request.getEnabled()));
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
        return toCategoryVO(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        AiSkillCategoryPO category = requireCategory(categoryId);
        category.setDeleted(1);
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
        categoryRelMapper.delete(new LambdaQueryWrapper<AiSkillCategoryRelPO>().eq(AiSkillCategoryRelPO::getCategoryId, categoryId));
    }

    private LambdaQueryWrapper<AiSkillPO> baseVisibleMarketWrapper(String keyword, String skillType, String subjectType, String targetAudience) {
        return new LambdaQueryWrapper<AiSkillPO>()
                .eq(AiSkillPO::getDeleted, 0)
                .eq(AiSkillPO::getVisibility, VISIBILITY_PUBLIC)
                .eq(AiSkillPO::getStatus, STATUS_PUBLISHED)
                .eq(AiSkillPO::getReviewStatus, REVIEW_APPROVED)
                .eq(StringUtils.hasText(skillType), AiSkillPO::getSkillType, normalizeSkillType(skillType))
                .eq(StringUtils.hasText(subjectType), AiSkillPO::getSubjectType, trimToNull(subjectType))
                .eq(StringUtils.hasText(targetAudience), AiSkillPO::getTargetAudience, trimToNull(targetAudience))
                .and(StringUtils.hasText(keyword), q -> q.like(AiSkillPO::getSkillName, keyword)
                        .or()
                        .like(AiSkillPO::getDescription, keyword)
                        .or()
                        .like(AiSkillPO::getUsageGuide, keyword))
                .orderByDesc(AiSkillPO::getUpdateTime)
                .orderByDesc(AiSkillPO::getId);
    }

    private AiSkillPO requireVisibleSkill(Long skillId, UserInfoDTO user) {
        AiSkillPO skill = requireSkill(skillId);
        if (isOwner(skill, user) || isAdmin(user) || isPublicPublished(skill)) {
            return skill;
        }
        throw new BaseException(HttpStatus.FORBIDDEN, "无权访问该Skill");
    }

    private AiSkillPO requirePublicPublishedSkill(Long skillId) {
        AiSkillPO skill = requireSkill(skillId);
        if (!isPublicPublished(skill)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "该Skill未公开发布");
        }
        return skill;
    }

    private AiSkillPO requireOwnerSkill(Long skillId, UserInfoDTO user) {
        AiSkillPO skill = requireSkill(skillId);
        if (!isOwner(skill, user)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "仅创建者可操作该Skill");
        }
        return skill;
    }

    private AiSkillPO requireSkill(Long skillId) {
        if (skillId == null || skillId <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "Skill ID不能为空");
        }
        AiSkillPO skill = skillMapper.selectOne(new LambdaQueryWrapper<AiSkillPO>()
                .eq(AiSkillPO::getId, skillId)
                .eq(AiSkillPO::getDeleted, 0));
        if (skill == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "Skill不存在");
        }
        return skill;
    }

    private UserInfoDTO requireUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private boolean isOwner(AiSkillPO skill, UserInfoDTO user) {
        return skill != null && user != null && skill.getUserId() != null && skill.getUserId().equals(user.getUserId());
    }

    private boolean isAdmin(UserInfoDTO user) {
        if (user == null || !StringUtils.hasText(user.getRoleCode())) {
            return false;
        }
        String role = user.getRoleCode().trim().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(role) || "SUPERADMIN".equals(role);
    }

    private boolean isPublicPublished(AiSkillPO skill) {
        return skill != null
                && VISIBILITY_PUBLIC.equals(skill.getVisibility())
                && STATUS_PUBLISHED.equals(skill.getStatus())
                && REVIEW_APPROVED.equals(skill.getReviewStatus())
                && Objects.equals(skill.getDeleted(), 0);
    }

    private SkillFileBundle validateAndNormalizeFiles(List<AiSkillFileDTO> files) {
        if (files == null || files.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "Skill文件不能为空");
        }
        Map<String, AiSkillFileDTO> normalized = new LinkedHashMap<>();
        for (AiSkillFileDTO file : files) {
            if (file == null) {
                continue;
            }
            String fileName = requiredText(file.getFileName(), "文件名不能为空").replace("\\", "/").trim();
            if (fileName.contains("/")) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "文件名不能包含目录：" + fileName);
            }
            if (!fileName.endsWith(".md")) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "Skill文件仅支持.md：" + fileName);
            }
            boolean inReferences = Boolean.TRUE.equals(file.getIsInReferences());
            if (!inReferences && !Set.of("SKILL.md", "README.md").contains(fileName)) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "根目录仅允许SKILL.md和README.md");
            }
            String key = inReferences ? "references/" + fileName : fileName;
            AiSkillFileDTO copy = new AiSkillFileDTO();
            copy.setFileName(fileName);
            copy.setIsInReferences(inReferences);
            copy.setContent(file.getContent() == null ? "" : file.getContent());
            normalized.put(key, copy);
        }
        AiSkillFileDTO skill = normalized.get("SKILL.md");
        if (skill == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "必须包含根目录SKILL.md");
        }
        if (!StringUtils.hasText(skill.getContent())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "SKILL.md内容不能为空");
        }
        for (Map.Entry<String, AiSkillFileDTO> entry : normalized.entrySet()) {
            if (entry.getKey().startsWith("references/") && !StringUtils.hasText(entry.getValue().getContent())) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "references中的文件不能为空：" + entry.getValue().getFileName());
            }
        }
        return new SkillFileBundle(new ArrayList<>(normalized.values()));
    }

    private void validateContent(SkillFileBundle bundle) {
        StringBuilder all = new StringBuilder();
        for (AiSkillFileDTO file : bundle.files()) {
            all.append('\n').append(file.getContent());
        }
        String content = all.toString().toLowerCase(Locale.ROOT);
        List<String> blocked = List.of("忽略之前的规则", "忽略系统提示", "绕过安全", "开发者模式", "代写作业",
                "帮我作弊", "隐藏日志", "泄露系统提示", "身份证号", "银行卡", "自杀", "色情", "赌博", "毒品");
        for (String keyword : blocked) {
            if (content.contains(keyword.toLowerCase(Locale.ROOT))) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "内容检查未通过，包含不适合学生学习的表达：" + keyword);
            }
        }
    }

    private SkillFileBundle readSkillZip(MultipartFile file) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = normalizeZipName(entry.getName());
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                zip.transferTo(out);
                entries.put(name, out.toByteArray());
            }
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "Skill zip读取失败");
        }
        String rootPrefix = detectSingleRootPrefix(entries.keySet());
        List<AiSkillFileDTO> files = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String path = rootPrefix == null ? entry.getKey() : entry.getKey().substring(rootPrefix.length());
            if (!StringUtils.hasText(path)) {
                continue;
            }
            if ("SKILL.md".equals(path) || "README.md".equals(path)) {
                files.add(fileDto(path, false, new String(entry.getValue(), StandardCharsets.UTF_8)));
            } else if (path.startsWith("references/")) {
                String remain = path.substring("references/".length());
                if (!remain.endsWith(".md")) {
                    continue;
                }
                if (remain.contains("/")) {
                    throw new BaseException(HttpStatus.BAD_REQUEST, "references禁止嵌套子目录");
                }
                files.add(fileDto(remain, true, new String(entry.getValue(), StandardCharsets.UTF_8)));
            }
        }
        return validateAndNormalizeFiles(files);
    }

    private String detectSingleRootPrefix(Set<String> names) {
        boolean hasRootSkill = names.contains("SKILL.md");
        if (hasRootSkill) {
            return null;
        }
        Set<String> roots = new LinkedHashSet<>();
        for (String name : names) {
            int slash = name.indexOf('/');
            if (slash > 0) {
                roots.add(name.substring(0, slash + 1));
            }
        }
        if (roots.size() == 1) {
            String root = roots.iterator().next();
            if (names.contains(root + "SKILL.md")) {
                return root;
            }
        }
        return null;
    }

    private AiSkillFileDTO fileDto(String fileName, boolean inReferences, String content) {
        AiSkillFileDTO dto = new AiSkillFileDTO();
        dto.setFileName(fileName);
        dto.setIsInReferences(inReferences);
        dto.setContent(content);
        return dto;
    }

    private String normalizeZipName(String name) {
        String value = name == null ? "" : name.replace("\\", "/");
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.contains("..")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "zip包含非法路径");
        }
        return value;
    }

    private String inferSkillName(SkillFileBundle bundle) {
        for (AiSkillFileDTO file : bundle.files()) {
            if ("SKILL.md".equals(file.getFileName())) {
                String content = file.getContent();
                for (String line : content.split("\\R")) {
                    if (line.startsWith("name:")) {
                        return line.substring("name:".length()).trim();
                    }
                    if (line.startsWith("# ")) {
                        return line.substring(2).trim();
                    }
                }
            }
        }
        return "导入的AI Skill";
    }

    private void saveCategoryRelations(Long skillId, List<Long> categoryIds) {
        categoryRelMapper.delete(new LambdaQueryWrapper<AiSkillCategoryRelPO>().eq(AiSkillCategoryRelPO::getSkillId, skillId));
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long categoryId : new LinkedHashSet<>(categoryIds)) {
            if (categoryId == null) {
                continue;
            }
            requireEnabledCategory(categoryId);
            categoryRelMapper.insert(AiSkillCategoryRelPO.builder()
                    .skillId(skillId)
                    .categoryId(categoryId)
                    .deleted(0)
                    .createTime(now)
                    .updateTime(now)
                    .build());
        }
    }

    private void writeFiles(String rootPath, List<AiSkillFileDTO> files) {
        ensureBucket();
        for (AiSkillFileDTO file : files) {
            String relativePath = Boolean.TRUE.equals(file.getIsInReferences())
                    ? "references/" + file.getFileName()
                    : file.getFileName();
            byte[] bytes = (file.getContent() == null ? "" : file.getContent()).getBytes(StandardCharsets.UTF_8);
            try (InputStream input = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName())
                        .object(rootPath + relativePath)
                        .contentType("text/markdown; charset=utf-8")
                        .stream(input, (long) bytes.length, -1L)
                        .build());
            } catch (Exception ex) {
                log.error("写入Skill文件失败，object={}", rootPath + relativePath, ex);
                throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Skill文件写入失败");
            }
        }
    }

    private List<AiSkillFileVO> readFiles(String rootPath, boolean includeContent) {
        if (!StringUtils.hasText(rootPath)) {
            return List.of();
        }
        List<AiSkillFileVO> files = new ArrayList<>();
        for (String objectName : listObjectNames(rootPath)) {
            String relative = objectName.substring(rootPath.length());
            if (!isAllowedStoredFile(relative)) {
                continue;
            }
            boolean inReferences = relative.startsWith("references/");
            String fileName = inReferences ? relative.substring("references/".length()) : relative;
            files.add(AiSkillFileVO.builder()
                    .fileName(fileName)
                    .isInReferences(inReferences)
                    .objectPath(objectName)
                    .content(includeContent ? readObjectAsString(objectName) : null)
                    .build());
        }
        files.sort(Comparator.comparing(AiSkillFileVO::getIsInReferences).thenComparing(AiSkillFileVO::getFileName));
        return files;
    }

    private boolean isAllowedStoredFile(String relative) {
        return "SKILL.md".equals(relative)
                || "README.md".equals(relative)
                || (relative.startsWith("references/")
                && relative.substring("references/".length()).endsWith(".md")
                && !relative.substring("references/".length()).contains("/"));
    }

    private byte[] buildZipBytes(AiSkillPO skill) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            List<AiSkillFileVO> files = readFiles(skill.getRootObjectPath(), true);
            for (AiSkillFileVO file : files) {
                String path = Boolean.TRUE.equals(file.getIsInReferences())
                        ? "references/" + file.getFileName()
                        : file.getFileName();
                zip.putNextEntry(new ZipEntry(path));
                zip.write((file.getContent() == null ? "" : file.getContent()).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Skill导出失败");
        }
    }

    private String buildInvokePrompt(AiSkillPO skill, List<AiSkillFileVO> files, String inputText) {
        StringBuilder builder = new StringBuilder();
        builder.append("你正在使用平台中的 AI Skill，请严格依据 Skill 的触发条件、执行步骤和参考资料回答。\n\n")
                .append("【Skill名称】").append(skill.getSkillName()).append('\n')
                .append("【技能类型】").append(skill.getSkillType()).append('\n');
        if (StringUtils.hasText(skill.getSubjectType())) {
            builder.append("【学科类型】").append(skill.getSubjectType()).append('\n');
        }
        if (StringUtils.hasText(skill.getTargetAudience())) {
            builder.append("【适用人群】").append(skill.getTargetAudience()).append('\n');
        }
        if (StringUtils.hasText(skill.getUsageGuide())) {
            builder.append("【使用方法】\n").append(skill.getUsageGuide()).append("\n\n");
        }
        appendSkillFile(builder, files, "SKILL.md", false, "【SKILL.md】");
        appendSkillFile(builder, files, "README.md", false, "【README.md】");
        List<AiSkillFileVO> references = files.stream()
                .filter(file -> Boolean.TRUE.equals(file.getIsInReferences()))
                .sorted(Comparator.comparing(AiSkillFileVO::getFileName))
                .toList();
        if (!references.isEmpty()) {
            builder.append("\n【references】\n");
            references.forEach(file -> builder.append("### ")
                    .append(file.getFileName())
                    .append('\n')
                    .append(file.getContent())
                    .append("\n\n"));
        }
        builder.append("【用户输入】\n").append(inputText).append('\n');
        return builder.toString();
    }

    private void appendSkillFile(StringBuilder builder, List<AiSkillFileVO> files, String fileName, boolean inReferences, String title) {
        files.stream()
                .filter(file -> Objects.equals(file.getFileName(), fileName))
                .filter(file -> Objects.equals(file.getIsInReferences(), inReferences))
                .findFirst()
                .ifPresent(file -> builder.append('\n')
                        .append(title)
                        .append('\n')
                        .append(file.getContent())
                        .append("\n"));
    }

    private String readObjectAsString(String objectName) {
        try (InputStream input = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName())
                .object(objectName)
                .build())) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "读取Skill文件失败");
        }
    }

    private List<String> listObjectNames(String prefix) {
        List<String> names = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName())
                    .prefix(prefix)
                    .recursive(true)
                    .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    names.add(item.objectName());
                }
            }
            return names;
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "读取Skill文件列表失败");
        }
    }

    private void removePrefixQuietly(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return;
        }
        try {
            for (String objectName : listObjectNames(prefix)) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName())
                        .object(objectName)
                        .build());
            }
        } catch (Exception ex) {
            log.warn("删除Skill MinIO目录失败，prefix={}", prefix, ex);
        }
    }

    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName()).build());
            }
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO存储桶初始化失败");
        }
    }

    private String bucketName() {
        String bucketName = minioProperties.getBuckerName();
        if (!StringUtils.hasText(bucketName)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO存储桶未配置");
        }
        return bucketName;
    }

    private String newRootPath(Long userId) {
        return ROOT_PREFIX + userId + "/" + UUID.randomUUID() + "/";
    }

    private AiSkillVO toVO(AiSkillPO skill, UserInfoDTO viewer, boolean includeFiles) {
        SysUserPO creator = skill.getUserId() == null ? null : sysUserRepository.selectUserById(skill.getUserId());
        return AiSkillVO.builder()
                .id(skill.getId())
                .userId(skill.getUserId())
                .creatorName(creatorName(creator))
                .skillName(skill.getSkillName())
                .description(skill.getDescription())
                .skillType(skill.getSkillType())
                .subjectType(skill.getSubjectType())
                .targetAudience(skill.getTargetAudience())
                .usageGuide(skill.getUsageGuide())
                .exampleInput(skill.getExampleInput())
                .exampleOutput(skill.getExampleOutput())
                .visibility(skill.getVisibility())
                .status(skill.getStatus())
                .reviewStatus(skill.getReviewStatus())
                .reviewMessage(skill.getReviewMessage())
                .rootObjectPath(skill.getRootObjectPath())
                .collected(viewer != null && viewer.getUserId() != null && isCollected(viewer.getUserId(), skill.getId()))
                .createTime(skill.getCreateTime())
                .updateTime(skill.getUpdateTime())
                .categories(listSkillCategories(skill.getId()))
                .files(includeFiles ? readFiles(skill.getRootObjectPath(), true) : readFiles(skill.getRootObjectPath(), false))
                .build();
    }

    private String creatorName(SysUserPO user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private boolean isCollected(Long userId, Long skillId) {
        return collectionMapper.exists(new LambdaQueryWrapper<AiSkillCollectionPO>()
                .eq(AiSkillCollectionPO::getUserId, userId)
                .eq(AiSkillCollectionPO::getSkillId, skillId)
                .eq(AiSkillCollectionPO::getDeleted, 0));
    }

    private List<AiSkillCategoryVO> listSkillCategories(Long skillId) {
        List<Long> categoryIds = categoryRelMapper.selectList(new LambdaQueryWrapper<AiSkillCategoryRelPO>()
                        .eq(AiSkillCategoryRelPO::getSkillId, skillId)
                        .eq(AiSkillCategoryRelPO::getDeleted, 0))
                .stream()
                .map(AiSkillCategoryRelPO::getCategoryId)
                .toList();
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<AiSkillCategoryPO>()
                        .eq(AiSkillCategoryPO::getDeleted, 0)
                        .in(AiSkillCategoryPO::getId, categoryIds))
                .stream()
                .map(this::toCategoryVO)
                .toList();
    }

    private AiSkillCategoryVO toCategoryVO(AiSkillCategoryPO category) {
        return AiSkillCategoryVO.builder()
                .id(category.getId())
                .categoryType(category.getCategoryType())
                .categoryName(category.getCategoryName())
                .sort(category.getSort())
                .enabled(category.getEnabled())
                .createTime(category.getCreateTime())
                .updateTime(category.getUpdateTime())
                .build();
    }

    private AiSkillCategoryPO requireCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "分类ID不能为空");
        }
        AiSkillCategoryPO category = categoryMapper.selectOne(new LambdaQueryWrapper<AiSkillCategoryPO>()
                .eq(AiSkillCategoryPO::getId, categoryId)
                .eq(AiSkillCategoryPO::getDeleted, 0));
        if (category == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private AiSkillCategoryPO requireEnabledCategory(Long categoryId) {
        AiSkillCategoryPO category = requireCategory(categoryId);
        if (!Objects.equals(category.getEnabled(), 1)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "分类已禁用：" + category.getCategoryName());
        }
        return category;
    }

    private String normalizeSkillType(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "OTHER";
        if (!SKILL_TYPES.contains(normalized)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "不支持的Skill类型：" + value);
        }
        return normalized;
    }

    private String normalizeVisibility(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : VISIBILITY_PRIVATE;
        if (!Set.of(VISIBILITY_PRIVATE, VISIBILITY_PUBLIC).contains(normalized)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "可见性必须是PRIVATE或PUBLIC");
        }
        return normalized;
    }

    private String normalizeStatus(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_OFFLINE).contains(normalized)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "不支持的状态：" + value);
        }
        return normalized;
    }

    private String normalizeCategoryType(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "OTHER";
        if (!CATEGORY_TYPES.contains(normalized)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "不支持的分类类型：" + value);
        }
        return normalized;
    }

    private Integer normalizeEnabled(Integer enabled) {
        return enabled != null && enabled == 0 ? 0 : 1;
    }

    private String requiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String sanitizeDownloadName(String value) {
        String name = StringUtils.hasText(value) ? value.trim() : "ai-skill";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private record SkillFileBundle(List<AiSkillFileDTO> files) {
    }
}
