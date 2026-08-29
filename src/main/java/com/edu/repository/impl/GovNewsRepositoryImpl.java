package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.mapper.EduGovNewsCategoryMapper;
import com.edu.mapper.EduGovNewsMapper;
import com.edu.pojo.po.EduGovNewsCategoryPO;
import com.edu.pojo.po.EduGovNewsPO;
import com.edu.repository.GovNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GovNewsRepositoryImpl implements GovNewsRepository {
    private static final int NOT_DELETED = 0;
    private static final int ENABLED = 1;
    private static final int PUBLISHED = 1;

    private final EduGovNewsCategoryMapper categoryMapper;
    private final EduGovNewsMapper newsMapper;

    @Override
    public List<EduGovNewsCategoryPO> selectPublicCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<EduGovNewsCategoryPO>()
                .eq(EduGovNewsCategoryPO::getDeleted, NOT_DELETED)
                .eq(EduGovNewsCategoryPO::getStatus, ENABLED)
                .orderByAsc(EduGovNewsCategoryPO::getSortOrder)
                .orderByAsc(EduGovNewsCategoryPO::getId));
    }

    @Override
    public List<EduGovNewsCategoryPO> selectAdminCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<EduGovNewsCategoryPO>()
                .eq(EduGovNewsCategoryPO::getDeleted, NOT_DELETED)
                .orderByAsc(EduGovNewsCategoryPO::getSortOrder)
                .orderByAsc(EduGovNewsCategoryPO::getId));
    }

    @Override
    public EduGovNewsCategoryPO selectCategoryById(Long categoryId) {
        return categoryMapper.selectOne(new LambdaQueryWrapper<EduGovNewsCategoryPO>()
                .eq(EduGovNewsCategoryPO::getId, categoryId)
                .eq(EduGovNewsCategoryPO::getDeleted, NOT_DELETED));
    }

    @Override
    public int insertCategory(EduGovNewsCategoryPO category) {
        return categoryMapper.insert(category);
    }

    @Override
    public int updateCategory(EduGovNewsCategoryPO category) {
        return categoryMapper.updateById(category);
    }

    @Override
    public IPage<EduGovNewsPO> selectPublicNewsPage(long pageNum, long pageSize, Long categoryId, String keyword) {
        return newsMapper.selectPage(new Page<>(pageNum, pageSize), publicNewsQuery()
                .eq(categoryId != null, EduGovNewsPO::getCategoryId, categoryId)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(EduGovNewsPO::getTitle, keyword.trim())
                        .or()
                        .like(EduGovNewsPO::getContentMd, keyword.trim()))
                .orderByDesc(EduGovNewsPO::getIsTop)
                .orderByDesc(EduGovNewsPO::getPublishedAt)
                .orderByDesc(EduGovNewsPO::getId));
    }

    @Override
    public IPage<EduGovNewsPO> selectAdminNewsPage(
            long pageNum,
            long pageSize,
            Long categoryId,
            String keyword,
            Integer status
    ) {
        return newsMapper.selectPage(new Page<>(pageNum, pageSize), new LambdaQueryWrapper<EduGovNewsPO>()
                .eq(EduGovNewsPO::getDeleted, NOT_DELETED)
                .eq(categoryId != null, EduGovNewsPO::getCategoryId, categoryId)
                .eq(status != null, EduGovNewsPO::getStatus, status)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(EduGovNewsPO::getTitle, keyword.trim())
                        .or()
                        .like(EduGovNewsPO::getContentMd, keyword.trim()))
                .orderByDesc(EduGovNewsPO::getUpdateTime)
                .orderByDesc(EduGovNewsPO::getId));
    }

    @Override
    public EduGovNewsPO selectPublicNewsById(Long newsId) {
        return newsMapper.selectOne(publicNewsQuery().eq(EduGovNewsPO::getId, newsId));
    }

    @Override
    public EduGovNewsPO selectAdminNewsById(Long newsId) {
        return newsMapper.selectOne(new LambdaQueryWrapper<EduGovNewsPO>()
                .eq(EduGovNewsPO::getId, newsId)
                .eq(EduGovNewsPO::getDeleted, NOT_DELETED));
    }

    @Override
    public int insertNews(EduGovNewsPO news) {
        return newsMapper.insert(news);
    }

    @Override
    public int updateNews(EduGovNewsPO news) {
        return newsMapper.updateById(news);
    }

    private LambdaQueryWrapper<EduGovNewsPO> publicNewsQuery() {
        return new LambdaQueryWrapper<EduGovNewsPO>()
                .eq(EduGovNewsPO::getDeleted, NOT_DELETED)
                .eq(EduGovNewsPO::getStatus, PUBLISHED);
    }
}
