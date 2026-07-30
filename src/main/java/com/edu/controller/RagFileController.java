package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.vo.rag.RagDocumentVO;
import com.edu.pojo.vo.rag.RagFilePreviewContentVO;
import com.edu.pojo.vo.rag.RagFilePreviewImagesVO;
import com.edu.service.RagFileAccessService;
import com.edu.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
public class RagFileController {
    private final RagService ragService;
    private final RagFileAccessService ragFileAccessService;

    @Operation(summary = "分页查询知识库文档")
    @GetMapping("/kb/documents")
    public Result<PageResult<RagDocumentVO>> pageKnowledgeBaseDocuments(@RequestParam("kb_id") @NotNull @Min(1) Long kbId,
                                                                        @RequestParam(required = false) Integer pageNum,
                                                                        @RequestParam(required = false) Integer pageSize,
                                                                        @RequestParam(value = "doc_type", required = false) String docType,
                                                                        @RequestParam(value = "doc_name", required = false) String docName) {
        return Result.setResult(HttpStatus.OK, "查询成功",
                ragService.pageKnowledgeBaseDocuments(kbId, pageNum, pageSize, docType, docName));
    }

    @Operation(summary = "查询公开知识库文档")
    @GetMapping("/kb/public/documents")
    public Result<List<RagDocumentVO>> listPublicKnowledgeBaseDocuments(@RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragService.listPublicKnowledgeBaseDocuments(kbId));
    }

    @Operation(summary = "获取RAG文件预览")
    @GetMapping("/files/preview")
    public ResponseEntity<byte[]> previewRagFile(@RequestParam("file_url") String fileUrl,
                                                 @RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        return ragFileAccessService.previewKnowledgeBaseDocument(kbId, fileUrl);
    }

    @Operation(summary = "获取RAG文件预览文本")
    @GetMapping("/files/preview-content")
    public Result<RagFilePreviewContentVO> previewRagFileContent(@RequestParam("file_url") String fileUrl,
                                                                 @RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragFileAccessService.previewKnowledgeBaseDocumentContent(kbId, fileUrl));
    }

    @Operation(summary = "获取RAG文件预览图片")
    @GetMapping("/files/preview-images")
    public Result<RagFilePreviewImagesVO> previewRagFileImages(@RequestParam("file_url") String fileUrl,
                                                               @RequestParam("kb_id") @NotNull @Min(1) Long kbId) {
        return Result.setResult(HttpStatus.OK, "查询成功", ragFileAccessService.previewKnowledgeBaseDocumentImages(kbId, fileUrl));
    }

    @Operation(summary = "上传RAG文件")
    @PostMapping("/files/upload")
    public Result<Void> uploadKnowledgeBaseDocument(HttpServletRequest request,
                                                    @RequestPart MultipartFile file,
                                                    @RequestParam(required = false) String description,
                                                    @RequestParam @NotNull @Min(1) Long kbId) {
        ragService.uploadKnowledgeBaseDocument(request, file, description, kbId);
        return Result.setResult(HttpStatus.OK, "上传成功");
    }

    @Operation(summary = "更新RAG文件信息")
    @PostMapping("/files/update")
    public Result<Void> updateKnowledgeBaseDocument(@RequestParam("kb_id") @NotNull @Min(1) Long kbId,
                                                    @RequestParam("doc_id") @NotNull @Min(1) Long docId,
                                                    @RequestParam("doc_name") @NotBlank String docName,
                                                    @RequestParam(required = false) String description) {
        ragService.updateKnowledgeBaseDocument(kbId, docId, docName, description);
        return Result.setResult(HttpStatus.OK, "更新成功");
    }

    @Operation(summary = "删除RAG文件")
    @PostMapping("/files/delete")
    public Result<Void> deleteKnowledgeBaseDocument(@RequestParam("kb_id") @NotNull @Min(1) Long kbId,
                                                    @RequestParam("doc_id") @NotNull @Min(1) Long docId) {
        ragService.deleteKnowledgeBaseDocument(kbId, docId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }
}
