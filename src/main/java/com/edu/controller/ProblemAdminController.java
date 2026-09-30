package com.edu.controller;

import com.edu.common.Result;
import com.edu.common.PageResult;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.problem.ProblemCategorySaveRequest;
import com.edu.pojo.dto.problem.ProblemAuthoringRequest;
import com.edu.pojo.dto.problem.ProblemStatusRequest;
import com.edu.pojo.vo.problem.ProblemDetailVO;
import com.edu.service.ProblemAuthoringService;
import com.edu.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/problem-solving")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
public class ProblemAdminController {
    private final ProblemAuthoringService problemAuthoringService;

    @GetMapping("/problems") public Result<PageResult<Map<String, Object>>> problems(@RequestParam(required = false) Integer pageNum, @RequestParam(required = false) Integer pageSize) { requireAdmin(); return Result.setResult(HttpStatus.OK, "查询成功", problemAuthoringService.problems(pageNum, pageSize)); }
    @GetMapping("/problems/{problemId}") public Result<ProblemDetailVO> problem(@PathVariable Long problemId) { requireAdmin(); return Result.setResult(HttpStatus.OK, "查询成功", problemAuthoringService.problem(problemId)); }
    @PutMapping(value = "/problems/{problemId}", consumes = "multipart/form-data")
    public Result<Void> updateProblem(@PathVariable Long problemId,
                                      @Valid @RequestPart("request") ProblemAuthoringRequest request,
                                      @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        requireAdmin(); problemAuthoringService.updateProblem(problemId, request, images); return Result.setResult(HttpStatus.OK, "题目已保存");
    }
    @GetMapping("/problems/{problemId}/images/{imageId}") public ResponseEntity<byte[]> problemImage(@PathVariable Long problemId, @PathVariable Long imageId) { requireAdmin(); return problemAuthoringService.getProblemImage(problemId, imageId); }
    @PutMapping("/problems/{problemId}/status") public Result<Void> problemStatus(@PathVariable Long problemId, @Valid @RequestBody ProblemStatusRequest request) { requireAdmin(); problemAuthoringService.updateProblemStatus(problemId, request.getStatus()); return Result.setResult(HttpStatus.OK, "状态已更新"); }
    @DeleteMapping("/problems/{problemId}") public Result<Void> deleteProblem(@PathVariable Long problemId) { requireAdmin(); problemAuthoringService.deleteProblem(problemId); return Result.setResult(HttpStatus.OK, "题目已删除"); }
    @GetMapping("/categories") public Result<PageResult<Map<String, Object>>> categories(@RequestParam(required = false) Integer pageNum, @RequestParam(required = false) Integer pageSize) { requireAdmin(); return Result.setResult(HttpStatus.OK, "查询成功", problemAuthoringService.managedCategories(pageNum, pageSize)); }
    @PostMapping("/categories") public Result<Long> createCategory(@Valid @RequestBody ProblemCategorySaveRequest request) { requireAdmin(); return Result.setResult(HttpStatus.CREATED, "分类已上架", problemAuthoringService.createCategory(request)); }
    @PutMapping("/categories/{categoryId}/status") public Result<Void> categoryStatus(@PathVariable Long categoryId, @Valid @RequestBody ProblemStatusRequest request) { requireAdmin(); problemAuthoringService.updateCategoryStatus(categoryId, request.getStatus()); return Result.setResult(HttpStatus.OK, "状态已更新"); }
    @DeleteMapping("/categories/{categoryId}") public Result<Void> deleteCategory(@PathVariable Long categoryId) { requireAdmin(); problemAuthoringService.deleteCategory(categoryId); return Result.setResult(HttpStatus.OK, "分类已删除"); }

    private void requireAdmin() {
        String role = SecurityUtil.getLoginUser() == null ? "" : SecurityUtil.getLoginUser().getRoleCode();
        if (!"ADMIN".equals(role) && !"SUPERADMIN".equals(role)) throw new BaseException(HttpStatus.FORBIDDEN, "仅管理员可管理分类和上下架");
    }
}
