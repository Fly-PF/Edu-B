package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.gov.GovQuestionSaveRequest;
import com.edu.pojo.vo.gov.GovQuestionAdminVO;
import com.edu.pojo.vo.gov.GovQuestionImportResultVO;
import com.edu.service.GovQuestionAdminService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/gov-questions")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
public class GovQuestionAdminController {
    private final GovQuestionAdminService govQuestionAdminService;

    @GetMapping
    public Result<PageResult<GovQuestionAdminVO>> pageQuestions(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "查询成功",
                govQuestionAdminService.pageQuestions(subject, questionType, status, keyword, pageNum, pageSize)
        );
    }

    @GetMapping("/{questionId}")
    public Result<GovQuestionAdminVO> getQuestion(@PathVariable Long questionId) {
        return Result.setResult(HttpStatus.OK, "查询成功", govQuestionAdminService.getQuestion(questionId));
    }

    @PostMapping
    public Result<GovQuestionAdminVO> createQuestion(@Valid @RequestBody GovQuestionSaveRequest request) {
        return Result.setResult(HttpStatus.CREATED, "题目已创建", govQuestionAdminService.createQuestion(request));
    }

    @PutMapping("/{questionId}")
    public Result<GovQuestionAdminVO> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody GovQuestionSaveRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "题目已保存", govQuestionAdminService.updateQuestion(questionId, request));
    }

    @DeleteMapping("/{questionId}")
    public Result<Void> deleteQuestion(@PathVariable Long questionId) {
        govQuestionAdminService.deleteQuestion(questionId);
        return Result.setResult(HttpStatus.OK, "题目已删除");
    }

    @PostMapping("/import")
    public Result<GovQuestionImportResultVO> importQuestions(@RequestBody List<GovQuestionSaveRequest> requests) {
        return Result.setResult(HttpStatus.CREATED, "导入完成", govQuestionAdminService.importQuestions(requests));
    }
}

