package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.practice.PracticeSubmitRequest;
import com.edu.pojo.vo.practice.PracticeListItemVO;
import com.edu.pojo.vo.practice.StudentPracticeDetailVO;
import com.edu.service.LearningPracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/learning-practices")
@PreAuthorize("hasAuthority('STUDENT')")
@Tag(name = "学生学习练习")
public class StudentLearningPracticeController {
    private final LearningPracticeService learningPracticeService;

    @Operation(summary = "查询学生练习列表")
    @GetMapping
    public Result<List<PracticeListItemVO>> listPractices() {
        return Result.setResult(HttpStatus.OK, "查询成功", learningPracticeService.listStudentPractices());
    }

    @Operation(summary = "查询练习题目")
    @GetMapping("/{practiceId}")
    public Result<StudentPracticeDetailVO> getPractice(@PathVariable Long practiceId) {
        return Result.setResult(HttpStatus.OK, "查询成功", learningPracticeService.getStudentPractice(practiceId));
    }

    @Operation(summary = "提交学生练习")
    @PostMapping("/{practiceId}/submissions")
    public Result<StudentPracticeDetailVO> submitPractice(
            @PathVariable Long practiceId,
            @Valid @RequestBody PracticeSubmitRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "练习已提交，等待老师批改", learningPracticeService.submitPractice(practiceId, request));
    }
}
