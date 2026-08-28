package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.gov.GovMaterialSaveRequest;
import com.edu.pojo.vo.gov.GovMaterialVO;
import com.edu.service.GovMaterialService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/gov-materials")
@Tag(name = "考公网盘资料管理")
public class GovMaterialAdminController {
    private final GovMaterialService materialService;

    @Operation(summary = "分页查询考公网盘资料")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<PageResult<GovMaterialVO>> listMaterials(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", materialService.listMaterialsForAdmin(categoryId, status, pageNum, pageSize));
    }

    @Operation(summary = "新增考公网盘资料")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<GovMaterialVO> createMaterial(@Valid @RequestPart("data") GovMaterialSaveRequest request,
                                                @RequestPart(value = "file", required = false) MultipartFile file) {
        return Result.setResult(HttpStatus.CREATED, "资料已创建", materialService.createMaterial(request, file));
    }

    @Operation(summary = "编辑考公网盘资料")
    @PutMapping("/{materialId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<GovMaterialVO> updateMaterial(
            @PathVariable Long materialId,
            @Valid @RequestPart("data") GovMaterialSaveRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return Result.setResult(HttpStatus.OK, "资料已保存", materialService.updateMaterial(materialId, request, file));
    }

    @Operation(summary = "发布考公网盘资料")
    @PutMapping("/{materialId}/publish")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<Void> publishMaterial(@PathVariable Long materialId) {
        materialService.publishMaterial(materialId);
        return Result.setResult(HttpStatus.OK, "资料已发布");
    }

    @Operation(summary = "下架考公网盘资料")
    @PutMapping("/{materialId}/withdraw")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<Void> withdrawMaterial(@PathVariable Long materialId) {
        materialService.withdrawMaterial(materialId);
        return Result.setResult(HttpStatus.OK, "资料已下架");
    }

    @Operation(summary = "删除考公网盘资料")
    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<Void> deleteMaterial(@PathVariable Long materialId) {
        materialService.deleteMaterial(materialId);
        return Result.setResult(HttpStatus.OK, "资料已删除");
    }
}
