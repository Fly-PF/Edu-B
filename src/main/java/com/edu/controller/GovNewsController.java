package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.vo.gov.GovNewsCategoryVO;
import com.edu.pojo.vo.gov.GovNewsDetailVO;
import com.edu.pojo.vo.gov.GovNewsListItemVO;
import com.edu.service.GovNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gov/news")
@Tag(name = "考公资讯与公告")
public class GovNewsController {
    private final GovNewsService govNewsService;

    @Operation(summary = "查询启用的资讯分类")
    @GetMapping("/categories")
    public Result<List<GovNewsCategoryVO>> listCategories() {
        return Result.setResult(HttpStatus.OK, "查询成功", govNewsService.listPublicCategories());
    }

    @Operation(summary = "分页查询已发布资讯")
    @GetMapping
    public Result<PageResult<GovNewsListItemVO>> pageNews(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "查询成功",
                govNewsService.pagePublicNews(categoryId, keyword, pageNum, pageSize)
        );
    }

    @Operation(summary = "查询已发布资讯详情")
    @GetMapping("/{newsId}")
    public Result<GovNewsDetailVO> getNews(@PathVariable Long newsId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govNewsService.getPublicNews(newsId));
    }
}
