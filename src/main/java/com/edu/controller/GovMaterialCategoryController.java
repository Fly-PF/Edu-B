package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.vo.gov.GovMaterialCategoryVO;
import com.edu.service.GovMaterialCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gov-material-categories")
@Tag(name = "考公资料分类")
public class GovMaterialCategoryController {
    private final GovMaterialCategoryService categoryService;

    @Operation(summary = "查询启用的考公资料分类")
    @GetMapping
    public Result<List<GovMaterialCategoryVO>> listEnabledCategories() {
        return Result.setResult(HttpStatus.OK, "查询成功", categoryService.listEnabledCategories());
    }
}
