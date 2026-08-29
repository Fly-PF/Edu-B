package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.pojo.vo.gov.GovNewsCategoryVO;
import com.edu.pojo.vo.gov.GovNewsDetailVO;
import com.edu.pojo.vo.gov.GovNewsListItemVO;
import com.edu.service.GovNewsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GovNewsControllerContractTest {
    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 8, 27, 9, 10, 11);
    private static final LocalDateTime UPDATE_TIME = LocalDateTime.of(2026, 8, 27, 10, 11, 12);
    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 8, 27, 8, 30, 45);
    private static final String CONTENT_MD = "# 2027 国考公告\n\n公式：$E=mc^2$\n\n![职位表](https://example.com/images/jobs.png)";

    private GovNewsService service;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = mock(GovNewsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GovNewsController(service)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void categoriesJsonUsesExactCamelCaseContract() throws Exception {
        when(service.listPublicCategories()).thenReturn(List.of(GovNewsCategoryVO.builder()
                .id(3L)
                .name("招考公告")
                .sortOrder(10)
                .status(1)
                .createTime(CREATE_TIME)
                .updateTime(UPDATE_TIME)
                .build()));

        MvcResult result = mockMvc.perform(get("/api/gov/news/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].sortOrder").value(10))
                .andExpect(jsonPath("$.data[0].createTime").value("2026-08-27T09:10:11"))
                .andExpect(jsonPath("$.data[0].updateTime").value("2026-08-27T10:11:12"))
                .andReturn();

        JsonNode category = read(result).path("data").path(0);
        assertThat(fieldNames(category)).containsExactlyInAnyOrder(
                "id", "name", "sortOrder", "status", "createTime", "updateTime"
        );
    }

    @Test
    void pageJsonUsesExactPageAndRecordContractWithoutContentMd() throws Exception {
        GovNewsListItemVO item = GovNewsListItemVO.builder()
                .id(101L)
                .categoryId(3L)
                .categoryName("招考公告")
                .title("2027 年中央机关公开招录公告")
                .summary("报名时间与职位安排")
                .coverUrl(null)
                .isTop(1)
                .status(1)
                .publishedAt(PUBLISHED_AT)
                .createTime(CREATE_TIME)
                .updateTime(UPDATE_TIME)
                .build();
        when(service.pagePublicNews(3L, "国考", 1, 10)).thenReturn(PageResult.<GovNewsListItemVO>builder()
                .records(List.of(item))
                .total(1L)
                .pageNum(1)
                .pageSize(10)
                .build());

        MvcResult result = mockMvc.perform(get("/api/gov/news")
                        .param("categoryId", "3")
                        .param("keyword", "国考")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].isTop").value(1))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("招考公告"))
                .andExpect(jsonPath("$.data.records[0].coverUrl").isEmpty())
                .andExpect(jsonPath("$.data.records[0].publishedAt").value("2026-08-27T08:30:45"))
                .andExpect(jsonPath("$.data.records[0].contentMd").doesNotExist())
                .andReturn();

        JsonNode page = read(result).path("data");
        assertThat(fieldNames(page)).containsExactlyInAnyOrder("records", "total", "pageNum", "pageSize");
        assertThat(fieldNames(page.path("records").path(0))).containsExactlyInAnyOrder(
                "id", "categoryId", "categoryName", "title", "summary", "coverUrl", "isTop", "status",
                "publishedAt", "createTime", "updateTime"
        );
        assertThat(page.path("records").path(0).path("isTop").isInt()).isTrue();
        assertThat(page.path("records").path(0).path("coverUrl").isNull()).isTrue();
    }

    @Test
    void detailJsonAddsOnlyContentMdAndPreservesMarkdownLatexAndImageUrl() throws Exception {
        GovNewsDetailVO detail = GovNewsDetailVO.builder()
                .id(101L)
                .categoryId(3L)
                .categoryName("招考公告")
                .title("2027 年中央机关公开招录公告")
                .summary("报名时间与职位安排")
                .contentMd(CONTENT_MD)
                .coverUrl(null)
                .isTop(0)
                .status(1)
                .publishedAt(PUBLISHED_AT)
                .createTime(CREATE_TIME)
                .updateTime(UPDATE_TIME)
                .build();
        when(service.getPublicNews(101L)).thenReturn(detail);

        MvcResult result = mockMvc.perform(get("/api/gov/news/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isTop").value(0))
                .andExpect(jsonPath("$.data.categoryName").value("招考公告"))
                .andExpect(jsonPath("$.data.publishedAt").value("2026-08-27T08:30:45"))
                .andExpect(jsonPath("$.data.contentMd").value(CONTENT_MD))
                .andReturn();

        JsonNode data = read(result).path("data");
        assertThat(fieldNames(data)).containsExactlyInAnyOrder(
                "id", "categoryId", "categoryName", "title", "summary", "contentMd", "coverUrl", "isTop",
                "status", "publishedAt", "createTime", "updateTime"
        );
        assertThat(data.path("contentMd").textValue()).isEqualTo(CONTENT_MD);
        assertThat(data.path("isTop").isInt()).isTrue();
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private Set<String> fieldNames(JsonNode node) {
        return StreamSupport.stream(
                        ((Iterable<String>) () -> node.fieldNames()).spliterator(), false)
                .collect(Collectors.toSet());
    }
}
