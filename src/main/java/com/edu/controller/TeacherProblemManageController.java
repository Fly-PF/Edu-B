package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.problem.ProblemStatusRequest;
import com.edu.pojo.dto.problem.ProblemAuthoringRequest;
import com.edu.pojo.vo.problem.ProblemDetailVO;
import com.edu.service.ProblemAuthoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/problem-solving")
@PreAuthorize("hasAuthority('TEACHER')")
public class TeacherProblemManageController {
    private final ProblemAuthoringService problemAuthoringService;

    @GetMapping("/problems")
    public Result<PageResult<Map<String, Object>>> problems(@RequestParam(required = false) Integer pageNum, @RequestParam(required = false) Integer pageSize) {
        return Result.setResult(HttpStatus.OK, "查询成功", problemAuthoringService.ownProblems(pageNum, pageSize));
    }

    @GetMapping("/problems/{problemId}")
    public Result<ProblemDetailVO> problem(@PathVariable Long problemId) {
        return Result.setResult(HttpStatus.OK, "查询成功", problemAuthoringService.ownProblem(problemId));
    }

    @PutMapping(value = "/problems/{problemId}", consumes = "multipart/form-data")
    public Result<Void> updateProblem(@PathVariable Long problemId,
                                      @Valid @RequestPart("request") ProblemAuthoringRequest request,
                                      @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        problemAuthoringService.updateOwnProblem(problemId, request, images);
        return Result.setResult(HttpStatus.OK, "题目已保存");
    }

    @GetMapping("/problems/{problemId}/images/{imageId}")
    public ResponseEntity<byte[]> problemImage(@PathVariable Long problemId, @PathVariable Long imageId) {
        problemAuthoringService.ownProblem(problemId);
        return problemAuthoringService.getProblemImage(problemId, imageId);
    }

    @PutMapping("/problems/{problemId}/status")
    public Result<Void> problemStatus(@PathVariable Long problemId, @Valid @RequestBody ProblemStatusRequest request) {
        problemAuthoringService.updateOwnProblemStatus(problemId, request.getStatus());
        return Result.setResult(HttpStatus.OK, "状态已更新");
    }

    @DeleteMapping("/problems/{problemId}")
    public Result<Void> deleteProblem(@PathVariable Long problemId) {
        problemAuthoringService.deleteOwnProblem(problemId);
        return Result.setResult(HttpStatus.OK, "题目已删除");
    }
}
