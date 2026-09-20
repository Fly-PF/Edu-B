package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.skill.AiSkillCategorySaveRequest;
import com.edu.pojo.vo.skill.AiSkillCategoryVO;
import com.edu.service.AiSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ai-skill-categories")
@Tag(name = "AI Skill分类管理")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
public class AiSkillAdminCategoryController {
    private final AiSkillService aiSkillService;

    @Operation(summary = "查询Skill分类")
    @GetMapping
    public Result<List<AiSkillCategoryVO>> listCategories(@RequestParam(required = false) String categoryType,
                                                          @RequestParam(required = false) Boolean enabledOnly) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiSkillService.listCategories(categoryType, enabledOnly));
    }

    @Operation(summary = "新增Skill分类")
    @PostMapping
    public Result<AiSkillCategoryVO> createCategory(@RequestBody AiSkillCategorySaveRequest request) {
        return Result.setResult(HttpStatus.CREATED, "创建成功", aiSkillService.createCategory(request));
    }

    @Operation(summary = "编辑Skill分类")
    @PutMapping("/{categoryId}")
    public Result<AiSkillCategoryVO> updateCategory(@PathVariable Long categoryId,
                                                    @RequestBody AiSkillCategorySaveRequest request) {
        return Result.setResult(HttpStatus.OK, "保存成功", aiSkillService.updateCategory(categoryId, request));
    }

    @Operation(summary = "删除Skill分类")
    @DeleteMapping("/{categoryId}")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        aiSkillService.deleteCategory(categoryId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }
}
