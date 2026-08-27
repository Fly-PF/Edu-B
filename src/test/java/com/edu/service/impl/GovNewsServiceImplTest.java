package com.edu.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.PageResult;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.gov.GovNewsCreateRequest;
import com.edu.pojo.po.EduGovNewsCategoryPO;
import com.edu.pojo.po.EduGovNewsPO;
import com.edu.pojo.vo.gov.GovNewsListItemVO;
import com.edu.repository.GovNewsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovNewsServiceImplTest {
    @Mock
    private GovNewsRepository repository;

    private GovNewsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GovNewsServiceImpl(repository);
        UserInfoDTO admin = UserInfoDTO.builder().userId(88L).roleCode("ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicCategoriesReturnRepositoryPublicSelection() {
        when(repository.selectPublicCategories()).thenReturn(List.of(category()));

        assertThat(service.listPublicCategories()).extracting("id", "status").containsExactly(tuple(3L, 1));
        verify(repository).selectPublicCategories();
    }

    @Test
    void publicPageUsesPageResultAndDoesNotExposeContentMd() {
        Page<EduGovNewsPO> page = new Page<>(2, 5, 11);
        page.setRecords(List.of(publishedNews()));
        when(repository.selectPublicNewsPage(2, 5, 3L, "招录")).thenReturn(page);
        when(repository.selectAdminCategories()).thenReturn(List.of(category()));

        PageResult<GovNewsListItemVO> result = service.pagePublicNews(3L, " 招录 ", 2, 5);

        assertThat(result.getTotal()).isEqualTo(11);
        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(5);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(GovNewsListItemVO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("contentMd");
    }

    @Test
    void draftDetailCannotBeReadByPublicUser() {
        when(repository.selectPublicNewsById(10L)).thenReturn(null);

        assertThatThrownBy(() -> service.getPublicNews(10L))
                .isInstanceOf(BaseException.class)
                .hasMessage("资讯不存在");
    }

    @Test
    void offlineDetailCannotBeReadByPublicUser() {
        when(repository.selectPublicNewsById(11L)).thenReturn(null);

        assertThatThrownBy(() -> service.getPublicNews(11L))
                .isInstanceOf(BaseException.class)
                .hasMessage("资讯不存在");
    }

    @Test
    void deletedDetailCannotBeReadByPublicUser() {
        when(repository.selectPublicNewsById(12L)).thenReturn(null);

        assertThatThrownBy(() -> service.getPublicNews(12L))
                .isInstanceOf(BaseException.class)
                .hasMessage("资讯不存在");
    }

    @Test
    void adminCreatesDraftWithAuditFields() {
        when(repository.selectCategoryById(3L)).thenReturn(category());
        GovNewsCreateRequest request = new GovNewsCreateRequest();
        request.setCategoryId(3L);
        request.setTitle(" 2027 国考公告 ");
        request.setSummary("摘要");
        request.setContentMd("# 正文");
        request.setCoverUrl("cover.png");
        request.setIsTop(1);

        service.createNews(request);

        ArgumentCaptor<EduGovNewsPO> captor = ArgumentCaptor.forClass(EduGovNewsPO.class);
        verify(repository).insertNews(captor.capture());
        EduGovNewsPO saved = captor.getValue();
        assertThat(saved.getStatus()).isZero();
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(saved.getTitle()).isEqualTo("2027 国考公告");
        assertThat(saved.getCreateBy()).isEqualTo(88L);
        assertThat(saved.getUpdateBy()).isEqualTo(88L);
        assertThat(saved.getDeleted()).isZero();
    }

    @Test
    void publishSetsStatusAndPublishedAtAndIsIdempotent() {
        EduGovNewsPO news = draftNews();
        when(repository.selectAdminNewsById(20L)).thenReturn(news);
        when(repository.selectCategoryById(3L)).thenReturn(category());

        service.publishNews(20L);
        LocalDateTime firstPublishedAt = news.getPublishedAt();
        service.publishNews(20L);

        assertThat(news.getStatus()).isEqualTo(1);
        assertThat(firstPublishedAt).isNotNull();
        assertThat(news.getPublishedAt()).isEqualTo(firstPublishedAt);
        verify(repository, times(1)).updateNews(news);
    }

    @Test
    void offlineSetsStatusAndIsIdempotent() {
        EduGovNewsPO news = publishedNews();
        news.setId(21L);
        when(repository.selectAdminNewsById(21L)).thenReturn(news);
        when(repository.selectCategoryById(3L)).thenReturn(category());

        service.offlineNews(21L);
        service.offlineNews(21L);

        assertThat(news.getStatus()).isEqualTo(2);
        verify(repository, times(1)).updateNews(news);
    }

    @Test
    void alreadyPublishedAndAlreadyOfflineDoNotWriteAgain() {
        EduGovNewsPO published = publishedNews();
        published.setId(30L);
        EduGovNewsPO offline = publishedNews();
        offline.setId(31L);
        offline.setStatus(2);
        when(repository.selectAdminNewsById(30L)).thenReturn(published);
        when(repository.selectAdminNewsById(31L)).thenReturn(offline);
        when(repository.selectCategoryById(3L)).thenReturn(category());

        service.publishNews(30L);
        service.offlineNews(31L);

        verify(repository, never()).updateNews(any());
    }

    private EduGovNewsCategoryPO category() {
        return EduGovNewsCategoryPO.builder().id(3L).name("招考公告").sortOrder(1).status(1).deleted(0).build();
    }

    private EduGovNewsPO draftNews() {
        EduGovNewsPO news = publishedNews();
        news.setId(20L);
        news.setStatus(0);
        news.setPublishedAt(null);
        return news;
    }

    private EduGovNewsPO publishedNews() {
        return EduGovNewsPO.builder()
                .id(10L)
                .categoryId(3L)
                .title("国考招录公告")
                .summary("摘要")
                .contentMd("正文包含报名安排")
                .coverUrl("cover.png")
                .isTop(1)
                .status(1)
                .publishedAt(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }
}
