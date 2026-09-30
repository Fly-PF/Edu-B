package com.edu.controller;

import com.edu.common.Result;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.problem.ProblemAuthoringRequest;
import com.edu.service.ProblemAuthoringService;
import com.edu.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/problem-authoring")
@PreAuthorize("hasAnyAuthority('TEACHER','ADMIN','SUPERADMIN')")
public class ProblemAuthoringController {
    private final ProblemAuthoringService problemAuthoringService;

    @GetMapping("/subjects")
    public Result<List<Map<String, Object>>> subjects() { requireAuthor(); return Result.setResult(HttpStatus.OK, "查询成功", problemAuthoringService.subjects()); }
    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories(@RequestParam(required = false) Long subjectId) { requireAuthor(); return Result.setResult(HttpStatus.OK, "查询成功", problemAuthoringService.categories(subjectId, false)); }
    @PostMapping(value = "/problems", consumes = "multipart/form-data")
    public Result<Long> create(@Valid @RequestPart("request") ProblemAuthoringRequest request,
                               @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        requireAuthor();
        return Result.setResult(HttpStatus.CREATED, "题目已上架", problemAuthoringService.createProblem(request, images));
    }

    private void requireAuthor() {
        String role = SecurityUtil.getLoginUser() == null ? "" : SecurityUtil.getLoginUser().getRoleCode();
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role) && !"SUPERADMIN".equals(role)) throw new BaseException(HttpStatus.FORBIDDEN, "仅教师或管理员可维护题目");
    }
}
