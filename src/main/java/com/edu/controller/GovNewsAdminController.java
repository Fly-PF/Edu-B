package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.gov.GovNewsCategoryCreateRequest;
import com.edu.pojo.dto.gov.GovNewsCategoryStatusRequest;
import com.edu.pojo.dto.gov.GovNewsCategoryUpdateRequest;
import com.edu.pojo.dto.gov.GovNewsCreateRequest;
import com.edu.pojo.dto.gov.GovNewsUpdateRequest;
import com.edu.pojo.vo.gov.GovNewsCategoryVO;
import com.edu.pojo.vo.gov.GovNewsDetailVO;
import com.edu.pojo.vo.gov.GovNewsListItemVO;
import com.edu.service.GovNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/gov/news")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "考公资讯与公告管理")
public class GovNewsAdminController {
    private final GovNewsService govNewsService;

    @Operation(summary = "查询全部资讯分类")
    @GetMapping("/categories")
    public Result<List<GovNewsCategoryVO>> listCategories() {
        return Result.setResult(HttpStatus.OK, "查询成功", govNewsService.listAdminCategories());
    }

    @Operation(summary = "新增资讯分类")
    @PostMapping("/categories")
    public Result<GovNewsCategoryVO> createCategory(@Valid @RequestBody GovNewsCategoryCreateRequest request) {
        return Result.setResult(HttpStatus.CREATED, "分类已创建", govNewsService.createCategory(request));
    }

    @Operation(summary = "修改资讯分类")
    @PatchMapping("/categories/{id}")
    public Result<GovNewsCategoryVO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody GovNewsCategoryUpdateRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "分类已更新", govNewsService.updateCategory(id, request));
    }

    @Operation(summary = "启用或停用资讯分类")
    @PatchMapping("/categories/{id}/status")
    public Result<GovNewsCategoryVO> updateCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody GovNewsCategoryStatusRequest request
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "分类状态已更新",
                govNewsService.updateCategoryStatus(id, request.getStatus())
        );
    }

    @Operation(summary = "分页查询全部资讯")
    @GetMapping
    public Result<PageResult<GovNewsListItemVO>> pageNews(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "查询成功",
                govNewsService.pageAdminNews(categoryId, keyword, status, pageNum, pageSize)
        );
    }

    @Operation(summary = "查询资讯详情")
    @GetMapping("/{id}")
    public Result<GovNewsDetailVO> getNews(@PathVariable Long id) {
        return Result.setResult(HttpStatus.OK, "查询成功", govNewsService.getAdminNews(id));
    }

    @Operation(summary = "新建资讯草稿")
    @PostMapping
    public Result<GovNewsDetailVO> createNews(@Valid @RequestBody GovNewsCreateRequest request) {
        return Result.setResult(HttpStatus.CREATED, "资讯草稿已创建", govNewsService.createNews(request));
    }

    @Operation(summary = "更新资讯")
    @PutMapping("/{id}")
    public Result<GovNewsDetailVO> updateNews(
            @PathVariable Long id,
            @Valid @RequestBody GovNewsUpdateRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "资讯已更新", govNewsService.updateNews(id, request));
    }

    @Operation(summary = "发布资讯")
    @PostMapping("/{id}/publish")
    public Result<GovNewsDetailVO> publishNews(@PathVariable Long id) {
        return Result.setResult(HttpStatus.OK, "资讯已发布", govNewsService.publishNews(id));
    }

    @Operation(summary = "下架资讯")
    @PostMapping("/{id}/offline")
    public Result<GovNewsDetailVO> offlineNews(@PathVariable Long id) {
        return Result.setResult(HttpStatus.OK, "资讯已下架", govNewsService.offlineNews(id));
    }
}
