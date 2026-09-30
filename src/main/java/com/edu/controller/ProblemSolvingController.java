package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.problem.ProblemExplainRequest;
import com.edu.pojo.dto.problem.ProblemSessionCreateRequest;
import com.edu.pojo.vo.problem.ProblemCategoryVO;
import com.edu.pojo.vo.problem.ProblemDetailVO;
import com.edu.pojo.vo.problem.ProblemExplanationVO;
import com.edu.pojo.vo.problem.ProblemListItemVO;
import com.edu.pojo.vo.problem.ProblemSubjectVO;
import com.edu.pojo.vo.problem.ProblemSessionVO;
import com.edu.service.ProblemSolvingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/problem-solving")
@PreAuthorize("hasAnyAuthority('STUDENT','TEACHER')")
@Tag(name = "讲题")
public class ProblemSolvingController {
    private final ProblemSolvingService problemSolvingService;

    @Operation(summary = "查询讲题科目")
    @GetMapping("/subjects")
    public Result<List<ProblemSubjectVO>> listSubjects() {
        return Result.setResult(HttpStatus.OK, "查询成功", problemSolvingService.listSubjects());
    }

    @Operation(summary = "查询讲题题型")
    @GetMapping("/categories")
    public Result<List<ProblemCategoryVO>> listCategories(@RequestParam(required = false) Long subjectId) {
        return Result.setResult(HttpStatus.OK, "查询成功", problemSolvingService.listCategories(subjectId));
    }

    @Operation(summary = "分页查询讲题题库")
    @GetMapping("/problems")
    public Result<PageResult<ProblemListItemVO>> pageProblems(
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String difficulty, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer pageNum, @RequestParam(required = false, name = "page") Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return Result.setResult(HttpStatus.OK, "查询成功", problemSolvingService.pageProblems(
                subjectId, categoryId, difficulty, keyword, pageNum == null ? page : pageNum, pageSize));
    }

    @Operation(summary = "查询讲题详情")
    @GetMapping("/problems/{problemId}")
    public Result<ProblemDetailVO> getProblem(@PathVariable Long problemId) {
        return Result.setResult(HttpStatus.OK, "查询成功", problemSolvingService.getProblem(problemId));
    }

    @Operation(summary = "获取讲题图片")
    @GetMapping("/problems/{problemId}/images/{imageId}")
    public ResponseEntity<byte[]> getProblemImage(@PathVariable Long problemId, @PathVariable Long imageId) {
        return problemSolvingService.getProblemImage(problemId, imageId);
    }

    @Operation(summary = "当前步骤 AI 追问")
    @PostMapping("/problems/{problemId}/explain")
    public Result<ProblemExplanationVO> explain(@PathVariable Long problemId, @Valid @RequestBody ProblemExplainRequest request) {
        return Result.setResult(HttpStatus.OK, "讲解完成", problemSolvingService.explain(problemId, request));
    }

    @Operation(summary = "创建讲题实时会话")
    @PostMapping("/sessions")
    public Result<ProblemSessionVO> createSession(@Valid @RequestBody ProblemSessionCreateRequest request) {
        return Result.setResult(HttpStatus.OK, "会话已创建", problemSolvingService.createSession(request));
    }
}
