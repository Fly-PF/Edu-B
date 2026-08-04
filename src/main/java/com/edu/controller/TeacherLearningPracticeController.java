package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.practice.PracticeAiDraftRequest;
import com.edu.pojo.dto.practice.PracticeReviewRequest;
import com.edu.pojo.dto.practice.PracticePublishRequest;
import com.edu.pojo.vo.practice.TeacherPracticeCourseVO;
import com.edu.pojo.vo.practice.TeacherPracticeSubmissionVO;
import com.edu.service.LearningPracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/learning-practices")
@PreAuthorize("hasAuthority('TEACHER')")
@Tag(name = "教师练习批改")
public class TeacherLearningPracticeController {
    private final LearningPracticeService learningPracticeService;

    @Operation(summary = "查询课程练习提交")
    @GetMapping("/submissions")
    public Result<List<TeacherPracticeSubmissionVO>> listSubmissions(@RequestParam(required = false) String status) {
        return Result.setResult(HttpStatus.OK, "查询成功", learningPracticeService.listTeacherSubmissions(status));
    }

    @Operation(summary = "查询单次学生练习提交")
    @GetMapping("/submissions/{submissionId}")
    public Result<TeacherPracticeSubmissionVO> getSubmission(@PathVariable Long submissionId) {
        return Result.setResult(HttpStatus.OK, "查询成功", learningPracticeService.getTeacherSubmission(submissionId));
    }

    @Operation(summary = "保存开放题 AI 辅助批改草稿")
    @PatchMapping("/submissions/{submissionId}/ai-draft")
    public Result<TeacherPracticeSubmissionVO> saveAiReviewDraft(
            @PathVariable Long submissionId,
            @Valid @RequestBody PracticeAiDraftRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "AI 批改建议已带回练习", learningPracticeService.saveAiReviewDraft(submissionId, request));
    }

    @Operation(summary = "查询可发布练习的课程")
    @GetMapping("/courses")
    public Result<List<TeacherPracticeCourseVO>> listCourses() {
        return Result.setResult(HttpStatus.OK, "查询成功", learningPracticeService.listTeacherPracticeCourses());
    }

    @Operation(summary = "发布课程练习")
    @PostMapping
    public Result<Long> publishPractice(@Valid @RequestBody PracticePublishRequest request) {
        return Result.setResult(HttpStatus.CREATED, "习题已发布", learningPracticeService.publishPractice(request));
    }

    @Operation(summary = "删除课程练习")
    @DeleteMapping("/{practiceId}")
    public Result<Void> deletePractice(@PathVariable Long practiceId) {
        learningPracticeService.deletePractice(practiceId);
        return Result.setResult(HttpStatus.OK, "练习及相关提交已删除");
    }

    @Operation(summary = "批改学生练习")
    @PatchMapping("/submissions/{submissionId}/review")
    public Result<TeacherPracticeSubmissionVO> reviewSubmission(
            @PathVariable Long submissionId,
            @Valid @RequestBody PracticeReviewRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "批改已保存", learningPracticeService.reviewSubmission(submissionId, request));
    }
}
