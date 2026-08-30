package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.gov.GovKnowledgeNodeCreateRequest;
import com.edu.pojo.dto.gov.GovKnowledgeNodeUpdateRequest;
import com.edu.pojo.dto.gov.GovKnowledgeCompareSaveRequest;
import com.edu.pojo.vo.gov.GovKnowledgeNodeVO;
import com.edu.pojo.vo.gov.GovKnowledgeCompareVO;
import com.edu.service.GovKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/admin/gov/knowledge")
@Tag(name = "考公知识库管理")
@Validated
public class GovKnowledgeAdminController {
    private final GovKnowledgeService govKnowledgeService;

    @Operation(summary = "查询知识树（管理端）")
    @GetMapping("/subjects/{subject}/tree")
    public Result<List<GovKnowledgeNodeVO>> listAdminKnowledgeTree(@PathVariable @NotBlank String subject,
                                                                    @RequestParam(required = false) String keyword) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.listAdminKnowledgeTree(subject, keyword));
    }

    @Operation(summary = "查询知识节点详情（管理端）")
    @GetMapping("/nodes/{nodeId}")
    public Result<GovKnowledgeNodeVO> getAdminKnowledgeNode(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.getAdminKnowledgeNode(nodeId));
    }
    @Operation(summary = "创建知识节点")
    @PostMapping("/nodes")
    public Result<GovKnowledgeNodeVO> createKnowledgeNode(@RequestBody @Valid GovKnowledgeNodeCreateRequest request) {
        return Result.setResult(HttpStatus.CREATED, "创建成功", govKnowledgeService.createKnowledgeNode(request));
    }

    @Operation(summary = "更新知识节点")
    @PutMapping("/nodes/{nodeId}")
    public Result<GovKnowledgeNodeVO> updateKnowledgeNode(@PathVariable @Min(1) Long nodeId,
                                                           @RequestBody @Valid GovKnowledgeNodeUpdateRequest request) {
        return Result.setResult(HttpStatus.OK, "更新成功", govKnowledgeService.updateKnowledgeNode(nodeId, request));
    }

    @Operation(summary = "删除知识节点")
    @DeleteMapping("/nodes/{nodeId}")
    public Result<Void> deleteKnowledgeNode(@PathVariable @Min(1) Long nodeId) {
        govKnowledgeService.deleteKnowledgeNode(nodeId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }

    @Operation(summary = "查询易混辨析（管理端）")
    @GetMapping("/nodes/{nodeId}/compare")
    public Result<List<GovKnowledgeCompareVO>> listAdminKnowledgeCompare(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.listAdminKnowledgeCompare(nodeId));
    }

    @Operation(summary = "保存易混辨析（管理端）")
    @PostMapping("/nodes/{nodeId}/compare")
    public Result<GovKnowledgeCompareVO> saveKnowledgeCompare(@PathVariable @Min(1) Long nodeId,
                                                              @RequestBody @Valid GovKnowledgeCompareSaveRequest request) {
        return Result.setResult(HttpStatus.CREATED, "创建成功", govKnowledgeService.saveKnowledgeCompare(nodeId, request));
    }

    @Operation(summary = "更新易混辨析（管理端）")
    @PutMapping("/compare/{compareId}")
    public Result<GovKnowledgeCompareVO> updateKnowledgeCompare(@PathVariable @Min(1) Long compareId,
                                                                @RequestBody @Valid GovKnowledgeCompareSaveRequest request) {
        return Result.setResult(HttpStatus.OK, "更新成功", govKnowledgeService.updateKnowledgeCompare(compareId, request));
    }

    @Operation(summary = "删除易混辨析（管理端）")
    @DeleteMapping("/compare/{compareId}")
    public Result<Void> deleteKnowledgeCompare(@PathVariable @Min(1) Long compareId) {
        govKnowledgeService.deleteKnowledgeCompare(compareId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }
}
