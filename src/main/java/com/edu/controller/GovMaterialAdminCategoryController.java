package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.gov.GovMaterialCategorySaveRequest;
import com.edu.pojo.vo.gov.GovMaterialCategoryVO;
import com.edu.service.GovMaterialCategoryService;
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
@RequestMapping("/api/admin/gov-material-categories")
@Tag(name = "考公资料分类管理")
public class GovMaterialAdminCategoryController {
    private final GovMaterialCategoryService categoryService;

    @Operation(summary = "查询考公资料分类列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<List<GovMaterialCategoryVO>> listCategories() {
        return Result.setResult(HttpStatus.OK, "查询成功", categoryService.listAllCategories());
    }

    @Operation(summary = "新增考公资料分类")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<GovMaterialCategoryVO> createCategory(@Valid @RequestBody GovMaterialCategorySaveRequest request) {
        return Result.setResult(HttpStatus.CREATED, "分类已创建", categoryService.createCategory(request));
    }

    @Operation(summary = "编辑考公资料分类")
    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<GovMaterialCategoryVO> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody GovMaterialCategorySaveRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "分类已保存", categoryService.updateCategory(categoryId, request));
    }

    @Operation(summary = "删除考公资料分类")
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return Result.setResult(HttpStatus.OK, "分类已删除");
    }
}
