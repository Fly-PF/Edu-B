package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.exception.BaseException;
import com.edu.mapper.EduGovMaterialCategoryMapper;
import com.edu.mapper.EduGovMaterialMapper;
import com.edu.common.properties.MinioProperties;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.gov.GovMaterialLinkDTO;
import com.edu.pojo.dto.gov.GovMaterialSaveRequest;
import com.edu.pojo.po.EduGovMaterialCategoryPO;
import com.edu.pojo.po.EduGovMaterialPO;
import com.edu.pojo.vo.gov.GovMaterialVO;
import com.edu.service.GovMaterialService;
import com.edu.repository.RagRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class GovMaterialServiceImpl implements GovMaterialService {
    private final EduGovMaterialMapper materialMapper;
    private final EduGovMaterialCategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;
    private final RagRepository ragRepository;
    private final MinioProperties minioProperties;
    private final MinioClient minioClient;

    @Override
    public List<GovMaterialVO> listPublishedMaterials(Long categoryId) {
        List<EduGovMaterialCategoryPO> enabledCategories = categoryMapper.selectList(new LambdaQueryWrapper<EduGovMaterialCategoryPO>()
                .eq(EduGovMaterialCategoryPO::getDeleted, 0)
                .eq(EduGovMaterialCategoryPO::getStatus, 1)
                .orderByDesc(EduGovMaterialCategoryPO::getSortOrder)
                .orderByDesc(EduGovMaterialCategoryPO::getCreateTime));
        if (enabledCategories.isEmpty()) {
            return List.of();
        }
        Map<Long, String> categoryNameMap = enabledCategories.stream()
                .collect(Collectors.toMap(EduGovMaterialCategoryPO::getId, EduGovMaterialCategoryPO::getName));

        LambdaQueryWrapper<EduGovMaterialPO> wrapper = new LambdaQueryWrapper<EduGovMaterialPO>()
                .eq(EduGovMaterialPO::getDeleted, 0)
                .eq(EduGovMaterialPO::getStatus, 1)
                .in(EduGovMaterialPO::getCategoryId, enabledCategories.stream().map(EduGovMaterialCategoryPO::getId).toList())
                .orderByDesc(EduGovMaterialPO::getSortOrder)
                .orderByDesc(EduGovMaterialPO::getCreateTime);
        if (categoryId != null) {
            wrapper.eq(EduGovMaterialPO::getCategoryId, categoryId);
        }

        return materialMapper.selectList(wrapper).stream()
                .map(material -> toView(material, categoryNameMap.get(material.getCategoryId())))
                .toList();
    }

    @Override
    public PageResult<GovMaterialVO> listMaterialsForAdmin(Long categoryId, Integer status, Integer pageNum, Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);

        List<EduGovMaterialCategoryPO> categories = categoryMapper.selectList(new LambdaQueryWrapper<EduGovMaterialCategoryPO>()
                .eq(EduGovMaterialCategoryPO::getDeleted, 0)
                .orderByDesc(EduGovMaterialCategoryPO::getSortOrder)
                .orderByDesc(EduGovMaterialCategoryPO::getCreateTime));
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(EduGovMaterialCategoryPO::getId, EduGovMaterialCategoryPO::getName));

        LambdaQueryWrapper<EduGovMaterialPO> wrapper = new LambdaQueryWrapper<EduGovMaterialPO>()
                .eq(EduGovMaterialPO::getDeleted, 0)
                .orderByDesc(EduGovMaterialPO::getSortOrder)
                .orderByDesc(EduGovMaterialPO::getCreateTime);
        if (categoryId != null) {
            wrapper.eq(EduGovMaterialPO::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(EduGovMaterialPO::getStatus, status);
        }

        IPage<EduGovMaterialPO> page = materialMapper.selectPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), wrapper);
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream()
                .map(material -> toView(material, categoryNameMap.get(material.getCategoryId())))
                .toList());
    }

    @Override
    @Transactional
    public GovMaterialVO createMaterial(GovMaterialSaveRequest request, MultipartFile file) {
        UserInfoDTO user = requireUser();
        requireCategory(request.getCategoryId());
        validateMaterialType(request.getMaterialType());
        validateMaterialContent(request.getMaterialType(), request.getLinks(), file, true);
        String fileUrl = request.getMaterialType() == 1 ? uploadGovFile(file) : null;
        EduGovMaterialPO material = EduGovMaterialPO.builder()
                .categoryId(request.getCategoryId())
                .title(request.getTitle().trim())
                .description(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null)
                .materialType(request.getMaterialType())
                .linksJson(request.getMaterialType() == 0 ? writeLinks(request.getLinks()) : null)
                .fileName(request.getMaterialType() == 1 ? file.getOriginalFilename() : null)
                .fileUrl(fileUrl)
                .status(request.getStatus() == null ? 0 : normalizeStatus(request.getStatus()))
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();
        try {
            materialMapper.insert(material);
        } catch (RuntimeException ex) {
            if (fileUrl != null) {
                ragRepository.deleteObject(fileUrl);
            }
            throw ex;
        }
        return toView(material, getCategoryName(material.getCategoryId()));
    }

    @Override
    @Transactional
    public GovMaterialVO updateMaterial(Long materialId, GovMaterialSaveRequest request, MultipartFile file) {
        UserInfoDTO user = requireUser();
        EduGovMaterialPO material = requireMaterial(materialId);
        requireCategory(request.getCategoryId());
        validateMaterialType(request.getMaterialType());
        if (!request.getMaterialType().equals(material.getMaterialType())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "资料类型不可修改");
        }
        validateMaterialContent(material.getMaterialType(), request.getLinks(), file, false);
        String oldFileUrl = material.getFileUrl();
        String newFileUrl = file == null || file.isEmpty() ? oldFileUrl : uploadGovFile(file);
        material.setCategoryId(request.getCategoryId());
        material.setTitle(request.getTitle().trim());
        material.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        if (material.getMaterialType() == 0) {
            material.setLinksJson(writeLinks(request.getLinks()));
            material.setFileName(null);
            material.setFileUrl(null);
        } else if (newFileUrl != null && !newFileUrl.equals(oldFileUrl)) {
            material.setLinksJson(null);
            material.setFileName(file.getOriginalFilename());
            material.setFileUrl(newFileUrl);
        }
        material.setSortOrder(request.getSortOrder() == null ? material.getSortOrder() : request.getSortOrder());
        if (request.getStatus() != null) {
            material.setStatus(normalizeStatus(request.getStatus()));
        }
        material.setUpdateBy(user.getUserId());
        material.setUpdateTime(LocalDateTime.now());
        try {
            materialMapper.updateById(material);
        } catch (RuntimeException ex) {
            if (newFileUrl != null && !newFileUrl.equals(oldFileUrl)) {
                ragRepository.deleteObject(newFileUrl);
            }
            throw ex;
        }
        if (oldFileUrl != null && !oldFileUrl.equals(material.getFileUrl())) {
            ragRepository.deleteObject(oldFileUrl);
        }
        return toView(material, getCategoryName(material.getCategoryId()));
    }

    @Override
    public ResponseEntity<byte[]> readPublishedFile(String fileUrl) {
        String baseUrl = minioProperties.getGov().getGovFilesBaseUrl();
        if (!StringUtils.hasText(fileUrl) || !StringUtils.hasText(baseUrl)
                || fileUrl.contains("..") || !fileUrl.startsWith(baseUrl)
                || !fileUrl.toLowerCase().endsWith(".pdf")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件地址错误");
        }
        boolean published = materialMapper.exists(new LambdaQueryWrapper<EduGovMaterialPO>()
                .eq(EduGovMaterialPO::getFileUrl, fileUrl)
                .eq(EduGovMaterialPO::getMaterialType, 1)
                .eq(EduGovMaterialPO::getStatus, 1)
                .eq(EduGovMaterialPO::getDeleted, 0));
        if (!published) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.getBuckerName())
                .object(fileUrl)
                .build())) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(StreamUtils.copyToByteArray(inputStream));
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "文件不存在");
        }
    }

    @Override
    @Transactional
    public void publishMaterial(Long materialId) {
        updateStatus(materialId, 1);
    }

    @Override
    @Transactional
    public void withdrawMaterial(Long materialId) {
        updateStatus(materialId, 2);
    }

    @Override
    @Transactional
    public void deleteMaterial(Long materialId) {
        UserInfoDTO user = requireUser();
        EduGovMaterialPO material = requireMaterial(materialId);
        material.setDeleted(1);
        material.setUpdateBy(user.getUserId());
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);
    }

    private void updateStatus(Long materialId, int status) {
        UserInfoDTO user = requireUser();
        EduGovMaterialPO material = requireMaterial(materialId);
        material.setStatus(status);
        material.setUpdateBy(user.getUserId());
        material.setUpdateTime(LocalDateTime.now());
        materialMapper.updateById(material);
    }

    private GovMaterialVO toView(EduGovMaterialPO material, String categoryName) {
        return GovMaterialVO.builder()
                .id(material.getId())
                .categoryId(material.getCategoryId())
                .categoryName(categoryName)
                .title(material.getTitle())
                .description(material.getDescription())
                .materialType(material.getMaterialType())
                .links(readLinks(material.getLinksJson()))
                .fileName(material.getFileName())
                .fileUrl(material.getFileUrl())
                .status(material.getStatus())
                .sortOrder(material.getSortOrder())
                .createTime(material.getCreateTime())
                .updateTime(material.getUpdateTime())
                .build();
    }

    private EduGovMaterialPO requireMaterial(Long materialId) {
        EduGovMaterialPO material = materialMapper.selectOne(new LambdaQueryWrapper<EduGovMaterialPO>()
                .eq(EduGovMaterialPO::getId, materialId)
                .eq(EduGovMaterialPO::getDeleted, 0));
        if (material == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "资料不存在");
        }
        return material;
    }

    private EduGovMaterialCategoryPO requireCategory(Long categoryId) {
        EduGovMaterialCategoryPO category = categoryMapper.selectOne(new LambdaQueryWrapper<EduGovMaterialCategoryPO>()
                .eq(EduGovMaterialCategoryPO::getId, categoryId)
                .eq(EduGovMaterialCategoryPO::getDeleted, 0)
                .eq(EduGovMaterialCategoryPO::getStatus, 1));
        if (category == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "所属分类不存在或已停用");
        }
        return category;
    }

    private String getCategoryName(Long categoryId) {
        EduGovMaterialCategoryPO category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private UserInfoDTO requireUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser(UserInfoDTO.class);
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private int normalizeStatus(Integer status) {
        if (status == null) {
            return 0;
        }
        return switch (status) {
            case 1 -> 1;
            case 2 -> 2;
            default -> 0;
        };
    }

    private List<GovMaterialLinkDTO> readLinks(String linksJson) {
        if (!StringUtils.hasText(linksJson)) {
            return List.of();
        }
        try {
            List<GovMaterialLinkDTO> list = objectMapper.readValue(linksJson, new TypeReference<List<GovMaterialLinkDTO>>() {});
            return list == null ? List.of() : list.stream()
                    .filter(link -> link != null && StringUtils.hasText(link.getPlatform()) && StringUtils.hasText(link.getUrl()))
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeLinks(List<GovMaterialLinkDTO> links) {
        try {
            List<GovMaterialLinkDTO> normalized = links == null ? List.of() : links.stream()
                    .filter(link -> link != null && StringUtils.hasText(link.getPlatform()) && StringUtils.hasText(link.getUrl()))
                    .peek(link -> {
                        link.setPlatform(link.getPlatform().trim().toUpperCase());
                        link.setUrl(link.getUrl().trim());
                        if (link.getAccessCode() != null) {
                            link.setAccessCode(link.getAccessCode().trim());
                        }
                    })
                    .toList();
            if (normalized.isEmpty()) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "请至少添加一个有效的网盘链接");
            }
            return objectMapper.writeValueAsString(normalized);
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "网盘链接保存失败");
        }
    }

    private void validateMaterialType(Integer materialType) {
        if (materialType == null || (materialType != 0 && materialType != 1)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "资料类型无效");
        }
    }

    private void validateMaterialContent(Integer materialType, List<GovMaterialLinkDTO> links,
                                         MultipartFile file, boolean creating) {
        if (materialType == 0) {
            if (file != null && !file.isEmpty()) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "网盘链接资料不能上传文件");
            }
            writeLinks(links);
        } else if (creating && (file == null || file.isEmpty())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "PDF文件不能为空");
        } else if (file != null && !file.isEmpty()) {
            validateGovFile(file);
        }
    }

    private String uploadGovFile(MultipartFile file) {
        validateGovFile(file);
        String baseUrl = minioProperties.getGov().getGovFilesBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "考公资料存储路径未配置");
        }
        baseUrl = baseUrl.trim();
        while (baseUrl.startsWith("/")) {
            baseUrl = baseUrl.substring(1);
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String objectName = baseUrl + "pdf/" + UUID.randomUUID() + ".pdf";
        ragRepository.uploadObject(file, objectName);
        return objectName;
    }

    private void validateGovFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "PDF文件不能为空");
        }
        DataSize maxSize = minioProperties.getGov().getMaxGovFileSize();
        if (maxSize == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "考公资料大小限制未配置");
        }
        if (file.getSize() > maxSize.toBytes()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件不能超过" + maxSize.toMegabytes() + "MB");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (!"pdf".equalsIgnoreCase(extension)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅支持PDF格式文件");
        }
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !"application/pdf".equalsIgnoreCase(contentType)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "文件类型与扩展名不匹配");
        }
    }
}
