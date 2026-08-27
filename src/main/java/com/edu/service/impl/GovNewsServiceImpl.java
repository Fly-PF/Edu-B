package com.edu.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.gov.GovNewsCategoryCreateRequest;
import com.edu.pojo.dto.gov.GovNewsCategoryUpdateRequest;
import com.edu.pojo.dto.gov.GovNewsCreateRequest;
import com.edu.pojo.dto.gov.GovNewsUpdateRequest;
import com.edu.pojo.po.EduGovNewsCategoryPO;
import com.edu.pojo.po.EduGovNewsPO;
import com.edu.pojo.vo.gov.GovNewsCategoryVO;
import com.edu.pojo.vo.gov.GovNewsDetailVO;
import com.edu.pojo.vo.gov.GovNewsListItemVO;
import com.edu.repository.GovNewsRepository;
import com.edu.service.GovNewsService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GovNewsServiceImpl implements GovNewsService {
    private static final int CATEGORY_ENABLED = 1;
    private static final int NEWS_DRAFT = 0;
    private static final int NEWS_PUBLISHED = 1;
    private static final int NEWS_OFFLINE = 2;
    private static final int NOT_DELETED = 0;

    private final GovNewsRepository repository;

    @Override
    public List<GovNewsCategoryVO> listPublicCategories() {
        return repository.selectPublicCategories().stream().map(this::toCategoryVO).toList();
    }

    @Override
    public PageResult<GovNewsListItemVO> pagePublicNews(
            Long categoryId,
            String keyword,
            Integer pageNum,
            Integer pageSize
    ) {
        validateOptionalId(categoryId, "资讯分类不正确");
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<EduGovNewsPO> page = repository.selectPublicNewsPage(
                pageQuery.getPageNum(), pageQuery.getPageSize(), categoryId, normalize(keyword));
        Map<Long, EduGovNewsCategoryPO> categories = categoryMap();
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream()
                .map(news -> toListItemVO(news, categories.get(news.getCategoryId())))
                .toList());
    }

    @Override
    public GovNewsDetailVO getPublicNews(Long newsId) {
        validateRequiredId(newsId, "资讯不存在");
        EduGovNewsPO news = repository.selectPublicNewsById(newsId);
        if (news == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "资讯不存在");
        }
        return toDetailVO(news, repository.selectCategoryById(news.getCategoryId()));
    }

    @Override
    public List<GovNewsCategoryVO> listAdminCategories() {
        return repository.selectAdminCategories().stream().map(this::toCategoryVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovNewsCategoryVO createCategory(GovNewsCategoryCreateRequest request) {
        Long userId = requireCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        EduGovNewsCategoryPO category = EduGovNewsCategoryPO.builder()
                .name(request.getName().trim())
                .sortOrder(request.getSortOrder())
                .status(CATEGORY_ENABLED)
                .createBy(userId)
                .updateBy(userId)
                .createTime(now)
                .updateTime(now)
                .deleted(NOT_DELETED)
                .build();
        repository.insertCategory(category);
        return toCategoryVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovNewsCategoryVO updateCategory(Long categoryId, GovNewsCategoryUpdateRequest request) {
        EduGovNewsCategoryPO category = requireCategory(categoryId);
        if (request.getName() != null) {
            if (!StringUtils.hasText(request.getName())) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "分类名称不能为空");
            }
            category.setName(request.getName().trim());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        category.setUpdateBy(requireCurrentUserId());
        category.setUpdateTime(LocalDateTime.now());
        repository.updateCategory(category);
        return toCategoryVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovNewsCategoryVO updateCategoryStatus(Long categoryId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "分类状态不正确");
        }
        EduGovNewsCategoryPO category = requireCategory(categoryId);
        if (!Objects.equals(category.getStatus(), status)) {
            category.setStatus(status);
            category.setUpdateBy(requireCurrentUserId());
            category.setUpdateTime(LocalDateTime.now());
            repository.updateCategory(category);
        }
        return toCategoryVO(category);
    }

    @Override
    public PageResult<GovNewsListItemVO> pageAdminNews(
            Long categoryId,
            String keyword,
            Integer status,
            Integer pageNum,
            Integer pageSize
    ) {
        validateOptionalId(categoryId, "资讯分类不正确");
        validateOptionalNewsStatus(status);
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<EduGovNewsPO> page = repository.selectAdminNewsPage(
                pageQuery.getPageNum(), pageQuery.getPageSize(), categoryId, normalize(keyword), status);
        Map<Long, EduGovNewsCategoryPO> categories = categoryMap();
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream()
                .map(news -> toListItemVO(news, categories.get(news.getCategoryId())))
                .toList());
    }

    @Override
    public GovNewsDetailVO getAdminNews(Long newsId) {
        EduGovNewsPO news = requireAdminNews(newsId);
        return toDetailVO(news, repository.selectCategoryById(news.getCategoryId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovNewsDetailVO createNews(GovNewsCreateRequest request) {
        EduGovNewsCategoryPO category = requireCategory(request.getCategoryId());
        Long userId = requireCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        EduGovNewsPO news = EduGovNewsPO.builder()
                .categoryId(category.getId())
                .title(request.getTitle().trim())
                .summary(normalize(request.getSummary()))
                .contentMd(request.getContentMd())
                .coverUrl(normalize(request.getCoverUrl()))
                .isTop(request.getIsTop())
                .status(NEWS_DRAFT)
                .publishedAt(null)
                .createBy(userId)
                .updateBy(userId)
                .createTime(now)
                .updateTime(now)
                .deleted(NOT_DELETED)
                .build();
        repository.insertNews(news);
        return toDetailVO(news, category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovNewsDetailVO updateNews(Long newsId, GovNewsUpdateRequest request) {
        EduGovNewsPO news = requireAdminNews(newsId);
        EduGovNewsCategoryPO category = requireCategory(request.getCategoryId());
        news.setCategoryId(category.getId());
        news.setTitle(request.getTitle().trim());
        news.setSummary(normalize(request.getSummary()));
        news.setContentMd(request.getContentMd());
        news.setCoverUrl(normalize(request.getCoverUrl()));
        news.setIsTop(request.getIsTop());
        news.setUpdateBy(requireCurrentUserId());
        news.setUpdateTime(LocalDateTime.now());
        repository.updateNews(news);
        return toDetailVO(news, category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovNewsDetailVO publishNews(Long newsId) {
        EduGovNewsPO news = requireAdminNews(newsId);
        if (!Objects.equals(news.getStatus(), NEWS_PUBLISHED)) {
            news.setStatus(NEWS_PUBLISHED);
            news.setPublishedAt(LocalDateTime.now());
            touchNews(news);
            repository.updateNews(news);
        }
        return toDetailVO(news, repository.selectCategoryById(news.getCategoryId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovNewsDetailVO offlineNews(Long newsId) {
        EduGovNewsPO news = requireAdminNews(newsId);
        if (!Objects.equals(news.getStatus(), NEWS_OFFLINE)) {
            news.setStatus(NEWS_OFFLINE);
            touchNews(news);
            repository.updateNews(news);
        }
        return toDetailVO(news, repository.selectCategoryById(news.getCategoryId()));
    }

    private EduGovNewsCategoryPO requireCategory(Long categoryId) {
        validateRequiredId(categoryId, "资讯分类不存在");
        EduGovNewsCategoryPO category = repository.selectCategoryById(categoryId);
        if (category == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "资讯分类不存在");
        }
        return category;
    }

    private EduGovNewsPO requireAdminNews(Long newsId) {
        validateRequiredId(newsId, "资讯不存在");
        EduGovNewsPO news = repository.selectAdminNewsById(newsId);
        if (news == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "资讯不存在");
        }
        return news;
    }

    private void touchNews(EduGovNewsPO news) {
        news.setUpdateBy(requireCurrentUserId());
        news.setUpdateTime(LocalDateTime.now());
    }

    private Long requireCurrentUserId() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user.getUserId();
    }

    private Map<Long, EduGovNewsCategoryPO> categoryMap() {
        return repository.selectAdminCategories().stream()
                .collect(Collectors.toMap(EduGovNewsCategoryPO::getId, Function.identity(), (left, right) -> left));
    }

    private GovNewsCategoryVO toCategoryVO(EduGovNewsCategoryPO category) {
        return GovNewsCategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .createTime(category.getCreateTime())
                .updateTime(category.getUpdateTime())
                .build();
    }

    private GovNewsListItemVO toListItemVO(EduGovNewsPO news, EduGovNewsCategoryPO category) {
        return GovNewsListItemVO.builder()
                .id(news.getId())
                .categoryId(news.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .title(news.getTitle())
                .summary(news.getSummary())
                .coverUrl(news.getCoverUrl())
                .isTop(news.getIsTop())
                .status(news.getStatus())
                .publishedAt(news.getPublishedAt())
                .createTime(news.getCreateTime())
                .updateTime(news.getUpdateTime())
                .build();
    }

    private GovNewsDetailVO toDetailVO(EduGovNewsPO news, EduGovNewsCategoryPO category) {
        return GovNewsDetailVO.builder()
                .id(news.getId())
                .categoryId(news.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .title(news.getTitle())
                .summary(news.getSummary())
                .contentMd(news.getContentMd())
                .coverUrl(news.getCoverUrl())
                .isTop(news.getIsTop())
                .status(news.getStatus())
                .publishedAt(news.getPublishedAt())
                .createTime(news.getCreateTime())
                .updateTime(news.getUpdateTime())
                .build();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateRequiredId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BaseException(HttpStatus.NOT_FOUND, message);
        }
    }

    private void validateOptionalId(Long id, String message) {
        if (id != null && id <= 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void validateOptionalNewsStatus(Integer status) {
        if (status != null && status != NEWS_DRAFT && status != NEWS_PUBLISHED && status != NEWS_OFFLINE) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "资讯状态不正确");
        }
    }
}
