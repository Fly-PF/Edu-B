package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.edu.mapper.EduGovNewsCategoryMapper;
import com.edu.mapper.EduGovNewsMapper;
import com.edu.pojo.po.EduGovNewsCategoryPO;
import com.edu.pojo.po.EduGovNewsPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovNewsRepositoryImplTest {
    @Mock
    private EduGovNewsCategoryMapper categoryMapper;
    @Mock
    private EduGovNewsMapper newsMapper;

    private GovNewsRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "gov-news-test");
        assistant.setCurrentNamespace("com.edu.mapper.GovNewsTestMapper");
        TableInfoHelper.initTableInfo(assistant, EduGovNewsCategoryPO.class);
        TableInfoHelper.initTableInfo(assistant, EduGovNewsPO.class);
        repository = new GovNewsRepositoryImpl(categoryMapper, newsMapper);
    }

    @Test
    void publicCategoriesFilterEnabledAndNotDeletedAndUseStableOrder() {
        when(categoryMapper.selectList(any())).thenReturn(List.of());

        repository.selectPublicCategories();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<EduGovNewsCategoryPO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(categoryMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertThat(sql).contains("deleted", "status", "sort_order ASC", "id ASC");
        assertThat(captor.getValue().getParamNameValuePairs()).containsValues(0, 1);
    }

    @Test
    void publicPageFiltersPublishedCategoryAndKeywordAgainstTitleOrBody() {
        when(newsMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>());

        repository.selectPublicNewsPage(1, 10, 7L, "考试");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<EduGovNewsPO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(newsMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertThat(sql).contains(
                "deleted", "status", "category_id", "title", "content_md",
                "is_top DESC", "published_at DESC", "id DESC"
        );
        assertThat(sql).containsIgnoringCase("OR");
        assertThat(captor.getValue().getParamNameValuePairs()).containsValues(0, 1, 7L, "%考试%");
    }

    @Test
    void publicDetailExplicitlyFiltersPublishedAndNotDeleted() {
        when(newsMapper.selectOne(any())).thenReturn(null);

        repository.selectPublicNewsById(9L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<EduGovNewsPO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(newsMapper).selectOne(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("deleted", "status", "id");
        assertThat(captor.getValue().getParamNameValuePairs()).containsValues(0, 1, 9L);
    }
}
