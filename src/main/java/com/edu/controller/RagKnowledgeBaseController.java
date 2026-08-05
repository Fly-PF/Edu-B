package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.vo.rag.RagKnowledgeBaseVO;
import com.edu.service.RagFileAccessService;
import com.edu.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
@Validated
@Tag(name = "RAG")
public class RagKnowledgeBaseController {
    private final RagService ragService;
    private final RagFileAccessService ragFileAccessService;

    @Operation(summary = "查询我的知识库")
    @GetMapping("/kb/my")
    public Result<List<RagKnowledgeBaseVO>> listMyKnowledgeBases(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) Integer status,
                                                                 @RequestParam(value = "is_public", required = false) Integer isPublic,
                                                                 @RequestParam(value = "kb_type", required = false) Integer kbType) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.listMyKnowledgeBases(keyword, status, isPublic, kbType));
    }

    @Operation(summary = "查询公开知识库")
    @GetMapping("/kb/public")
    public Result<List<RagKnowledgeBaseVO>> listPublicKnowledgeBases(@RequestParam(value = "kb_type") Integer kbType,
                                                                     @RequestParam(defaultValue = "4") Integer limit) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.listPublicKnowledgeBases(kbType, limit));
    }

    @Operation(summary = "分页查询公开知识库")
    @GetMapping("/kb/public/page")
    public Result<PageResult<RagKnowledgeBaseVO>> pagePublicKnowledgeBases(@RequestParam(required = false) String keyword,
                                                                           @RequestParam(value = "kb_type", required = false) Integer kbType,
                                                                           @RequestParam(required = false) Integer pageNum,
                                                                           @RequestParam(required = false) Integer pageSize) {
        return Result.setResult(HttpStatus.OK, "查询成功",
                ragService.pagePublicKnowledgeBases(keyword, kbType, pageNum, pageSize));
    }

    @Operation(summary = "分页查询我的收藏知识库")
    @GetMapping("/kb/collection/page")
    public Result<PageResult<RagKnowledgeBaseVO>> pageCollectedKnowledgeBases(@RequestParam(required = false) String keyword,
                                                                              @RequestParam(value = "kb_type", required = false) Integer kbType,
                                                                              @RequestParam(required = false) Integer pageNum,
                                                                              @RequestParam(required = false) Integer pageSize) {
        return Result.setResult(HttpStatus.OK, "查询成功",
                ragService.pageCollectedKnowledgeBases(keyword, kbType, pageNum, pageSize));
    }

    @Operation(summary = "获取我的知识库详情")
    @GetMapping("/kb/my/detail")
    public Result<RagKnowledgeBaseVO> getMyKnowledgeBase(@RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.getMyKnowledgeBase(kbId));
    }

    @Operation(summary = "查询知识库收藏状态")
    @GetMapping("/kb/collection/status")
    public Result<Boolean> isKnowledgeBaseCollected(@RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.isKnowledgeBaseCollected(kbId));
    }

    @Operation(summary = "收藏知识库")
    @PostMapping("/kb/collection")
    public Result<Void> collectKnowledgeBase(@RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        ragService.collectKnowledgeBase(kbId);
        return Result.setResult(HttpStatus.OK, "收藏成功");
    }

    @Operation(summary = "取消收藏知识库")
    @PostMapping("/kb/collection/cancel")
    public Result<Void> cancelKnowledgeBaseCollection(@RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        ragService.cancelKnowledgeBaseCollection(kbId);
        return Result.setResult(HttpStatus.OK, "取消收藏成功");
    }

    @Operation(summary = "创建知识库")
    @PostMapping("/kb")
    public Result<Void> createKnowledgeBase(@RequestParam("kb_name") String kbName,
                                            @RequestParam(value = "description", required = false) String description,
                                            @RequestParam("kb_type") Integer kbType,
                                            @RequestParam(value = "is_public") Integer isPublic,
                                            @RequestPart(value = "kb_cover") MultipartFile file) {
        ragService.createKnowledgeBase(kbName, description, kbType, isPublic, file);
        return Result.setResult(HttpStatus.CREATED, "创建成功");
    }

    @Operation(summary = "更新知识库")
    @PostMapping("/kb/update")
    public Result<Void> updateKnowledgeBase(@RequestParam("kb_id") @NotNull @Min(1) Long kbId,
                                            @RequestParam("kb_name") String kbName,
                                            @RequestParam(value = "description", required = false) String description,
                                            @RequestParam("kb_type") Integer kbType,
                                            @RequestParam("is_public") Integer isPublic,
                                            @RequestParam("status") Integer status,
                                            @RequestPart(value = "kb_cover", required = false) MultipartFile file) {
        ragService.updateKnowledgeBase(kbId, kbName, description, kbType, isPublic, status, file);
        return Result.setResult(HttpStatus.OK, "更新成功");
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/kb/{kbId}")
    public Result<Void> deleteKnowledgeBase(@PathVariable("kbId") @NotNull @Min(1) Long kbId) {
        ragService.deleteKnowledgeBase(kbId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }

    @Operation(summary = "获取知识库封面")
    @GetMapping("/kb/cover")
    public ResponseEntity<byte[]> getKnowledgeBaseCover(@RequestParam String objectName) {
        return ragFileAccessService.getKnowledgeBaseCover(objectName);
    }
}
