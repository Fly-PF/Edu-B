package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;
import com.edu.service.TeacherAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/ai")
@PreAuthorize("hasAuthority('TEACHER')")
@Tag(name = "教师 AI 备课与批改")
public class TeacherAiController {
    private final TeacherAiService teacherAiService;

    @Operation(summary = "生成结构化教案")
    @PostMapping("/lesson-plans/generate")
    public Result<LessonPlanGenerateResponse> generateLessonPlan(
            @Valid @RequestBody LessonPlanGenerateRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "教案生成成功", teacherAiService.generateLessonPlan(request));
    }

    @Operation(summary = "生成结构化批改结果")
    @PostMapping("/gradings/generate")
    public Result<GradingGenerateResponse> generateGrading(
            @Valid @RequestBody GradingGenerateRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "批改完成", teacherAiService.generateGrading(request));
    }
}
