package com.edu.controller;

import com.edu.common.Result;
import com.edu.common.PageResult;
import com.edu.pojo.dto.gov.GovKnowledgeCompareSaveRequest;
import com.edu.pojo.dto.gov.GovKnowledgeAnnotationSaveRequest;
import com.edu.pojo.dto.gov.GovKnowledgeNoteSaveRequest;
import com.edu.pojo.dto.gov.GovKnowledgeProgressUpdateRequest;
import com.edu.pojo.vo.gov.GovKnowledgeCompareVO;
import com.edu.pojo.vo.gov.GovKnowledgeAnnotationVO;
import com.edu.pojo.vo.gov.GovKnowledgeFavoriteItemVO;
import com.edu.pojo.vo.gov.GovKnowledgeFavoriteVO;
import com.edu.pojo.vo.gov.GovKnowledgeNodeVO;
import com.edu.pojo.vo.gov.GovKnowledgeNoteItemVO;
import com.edu.pojo.vo.gov.GovKnowledgeNoteVO;
import com.edu.pojo.vo.gov.GovKnowledgeProgressVO;
import com.edu.service.GovKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gov/knowledge")
@Tag(name = "考公知识库")
@Validated
public class GovKnowledgeController {
    private final GovKnowledgeService govKnowledgeService;

    @Operation(summary = "查询知识树")
    @GetMapping("/subjects/{subject}/tree")
    public Result<List<GovKnowledgeNodeVO>> listKnowledgeTree(@PathVariable @NotBlank String subject,
                                                              @RequestParam(required = false) String keyword) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.listKnowledgeTree(subject, keyword));
    }

    @Operation(summary = "查询知识点详情")
    @GetMapping("/nodes/{nodeId}")
    public Result<GovKnowledgeNodeVO> getKnowledgeNode(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.getKnowledgeNode(nodeId));
    }

    @Operation(summary = "查询知识点学习进度")
    @GetMapping("/nodes/{nodeId}/progress")
    public Result<GovKnowledgeProgressVO> getKnowledgeProgress(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.getKnowledgeProgress(nodeId));
    }

    @Operation(summary = "更新知识点学习进度")
    @PostMapping("/nodes/{nodeId}/progress")
    public Result<GovKnowledgeProgressVO> updateKnowledgeProgress(@PathVariable @Min(1) Long nodeId,
                                                                  @RequestBody @Validated GovKnowledgeProgressUpdateRequest request) {
        return Result.setResult(HttpStatus.OK, "保存成功", govKnowledgeService.updateKnowledgeProgress(nodeId, request));
    }

    @Operation(summary = "查询知识点易混辨析")
    @GetMapping("/nodes/{nodeId}/compare")
    public Result<List<GovKnowledgeCompareVO>> listKnowledgeCompare(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.listKnowledgeCompare(nodeId));
    }

    @Operation(summary = "保存知识点易混辨析")
    @PostMapping("/nodes/{nodeId}/compare")
    public Result<GovKnowledgeCompareVO> saveKnowledgeCompare(@PathVariable @Min(1) Long nodeId,
                                                              @RequestBody @Validated GovKnowledgeCompareSaveRequest request) {
        return Result.setResult(HttpStatus.OK, "保存成功", govKnowledgeService.saveKnowledgeCompare(nodeId, request));
    }

    @Operation(summary = "更新知识点易混辨析")
    @PostMapping("/compare/{compareId}")
    public Result<GovKnowledgeCompareVO> updateKnowledgeCompare(@PathVariable @Min(1) Long compareId,
                                                                @RequestBody @Validated GovKnowledgeCompareSaveRequest request) {
        return Result.setResult(HttpStatus.OK, "更新成功", govKnowledgeService.updateKnowledgeCompare(compareId, request));
    }

    @Operation(summary = "删除知识点易混辨析")
    @DeleteMapping("/compare/{compareId}")
    public Result<Void> deleteKnowledgeCompare(@PathVariable @Min(1) Long compareId) {
        govKnowledgeService.deleteKnowledgeCompare(compareId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }

    @Operation(summary = "查询知识点收藏状态")
    @GetMapping("/nodes/{nodeId}/favorite")
    public Result<GovKnowledgeFavoriteVO> getKnowledgeFavorite(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.getKnowledgeFavorite(nodeId));
    }

    @Operation(summary = "收藏知识点")
    @PostMapping("/nodes/{nodeId}/favorite")
    public Result<GovKnowledgeFavoriteVO> collectKnowledge(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "收藏成功", govKnowledgeService.collectKnowledge(nodeId));
    }

    @Operation(summary = "取消收藏知识点")
    @PostMapping("/nodes/{nodeId}/favorite/cancel")
    public Result<GovKnowledgeFavoriteVO> cancelKnowledgeFavorite(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "取消收藏成功", govKnowledgeService.cancelKnowledgeFavorite(nodeId));
    }

    @Operation(summary = "查询知识点笔记")
    @GetMapping("/nodes/{nodeId}/note")
    public Result<GovKnowledgeNoteVO> getKnowledgeNote(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.getKnowledgeNote(nodeId));
    }

    @Operation(summary = "保存知识点笔记")
    @PostMapping("/nodes/{nodeId}/note")
    public Result<GovKnowledgeNoteVO> saveKnowledgeNote(@PathVariable @Min(1) Long nodeId,
                                                        @RequestBody @Validated GovKnowledgeNoteSaveRequest request) {
        return Result.setResult(HttpStatus.OK, "保存成功", govKnowledgeService.saveKnowledgeNote(nodeId, request));
    }

    @Operation(summary = "查询知识点正文标注")
    @GetMapping("/nodes/{nodeId}/annotations")
    public Result<List<GovKnowledgeAnnotationVO>> listKnowledgeAnnotations(@PathVariable @Min(1) Long nodeId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.listKnowledgeAnnotations(nodeId));
    }

    @Operation(summary = "保存知识点正文标注")
    @PostMapping("/nodes/{nodeId}/annotations")
    public Result<GovKnowledgeAnnotationVO> saveKnowledgeAnnotation(@PathVariable @Min(1) Long nodeId,
                                                                    @RequestBody @Validated GovKnowledgeAnnotationSaveRequest request) {
        return Result.setResult(HttpStatus.OK, "保存成功", govKnowledgeService.saveKnowledgeAnnotation(nodeId, request));
    }

    @Operation(summary = "更新知识点正文标注")
    @PostMapping("/annotations/{annotationId}")
    public Result<GovKnowledgeAnnotationVO> updateKnowledgeAnnotation(@PathVariable @Min(1) Long annotationId,
                                                                       @RequestBody @Validated GovKnowledgeAnnotationSaveRequest request) {
        return Result.setResult(HttpStatus.OK, "更新成功", govKnowledgeService.updateKnowledgeAnnotation(annotationId, request));
    }

    @Operation(summary = "删除知识点正文标注")
    @DeleteMapping("/annotations/{annotationId}")
    public Result<Void> deleteKnowledgeAnnotation(@PathVariable @Min(1) Long annotationId) {
        govKnowledgeService.deleteKnowledgeAnnotation(annotationId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }

    @Operation(summary = "分页查询我的收藏知识点")
    @GetMapping("/favorites/page")
    public Result<PageResult<GovKnowledgeFavoriteItemVO>> pageMyFavoriteKnowledge(@RequestParam(required = false) Integer pageNum,
                                                                                   @RequestParam(required = false) Integer pageSize,
                                                                                   @RequestParam(required = false) String keyword) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.pageMyFavoriteKnowledge(pageNum, pageSize, keyword));
    }

    @Operation(summary = "分页查询我的笔记")
    @GetMapping("/notes/page")
    public Result<PageResult<GovKnowledgeNoteItemVO>> pageMyKnowledgeNotes(@RequestParam(required = false) Integer pageNum,
                                                                           @RequestParam(required = false) Integer pageSize,
                                                                           @RequestParam(required = false) String keyword) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.pageMyKnowledgeNotes(pageNum, pageSize, keyword));
    }

    @Operation(summary = "分页查询我的标注")
    @GetMapping("/annotations/page")
    public Result<PageResult<GovKnowledgeAnnotationVO>> pageMyKnowledgeAnnotations(@RequestParam(required = false) Integer pageNum,
                                                                                   @RequestParam(required = false) Integer pageSize,
                                                                                   @RequestParam(required = false) String keyword) {
        return Result.setResult(HttpStatus.OK, "查询成功", govKnowledgeService.pageMyKnowledgeAnnotations(pageNum, pageSize, keyword));
    }
}
