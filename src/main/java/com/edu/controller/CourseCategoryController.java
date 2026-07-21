package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.course.CourseCategorySaveRequest;
import com.edu.pojo.vo.course.CourseCategoryVO;
import com.edu.service.CourseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course-categories")
@Tag(name = "课程展示分类")
public class CourseCategoryController {
    private final CourseCategoryService categoryService;

    @Operation(summary = "查询课程展示分类")
    @GetMapping
    public Result<List<CourseCategoryVO>> listCategories() {
        return Result.setResult(HttpStatus.OK, "查询成功", categoryService.listPublicCategories());
    }

    @Operation(summary = "查询可选课程标签")
    @GetMapping("/tags")
    public Result<List<String>> listTags() {
        return Result.setResult(HttpStatus.OK, "查询成功", categoryService.listAvailableTags());
    }

    @Operation(summary = "新增课程展示分类")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<CourseCategoryVO> createCategory(@Valid @RequestBody CourseCategorySaveRequest request) {
        return Result.setResult(HttpStatus.CREATED, "分类已创建", categoryService.createCategory(request));
    }

    @Operation(summary = "编辑课程展示分类")
    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<CourseCategoryVO> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CourseCategorySaveRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "分类已保存", categoryService.updateCategory(categoryId, request));
    }

    @Operation(summary = "删除课程展示分类")
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return Result.setResult(HttpStatus.OK, "分类已删除");
    }
}
