package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.learning.LearningCaseGenerateRequest;
import com.edu.pojo.dto.learning.LearningAssistantRequest;
import com.edu.pojo.dto.learning.LearningEvidenceSubmitRequest;
import com.edu.pojo.dto.learning.LearningPlanDecisionRequest;
import com.edu.pojo.dto.learning.LearningPlanReviewRequest;
import com.edu.pojo.vo.learning.LearningGrowthCaseVO;
import com.edu.pojo.vo.learning.LearningAssistantReplyVO;
import com.edu.pojo.vo.learning.LearningStudentGrowthVO;
import com.edu.pojo.vo.learning.LearningTeacherGrowthVO;
import com.edu.service.learning.LearningAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning-analysis")
@Tag(name = "AI学习诊断与成长闭环")
public class LearningAnalysisController {
    private final LearningAnalysisService learningAnalysisService;

    @Operation(summary = "教师查看AI学情干预中心")
    @GetMapping("/teacher/classes/{classId}/dashboard")
    public Result<LearningTeacherGrowthVO> getTeacherDashboard(@PathVariable Long classId) {
        return Result.setResult(HttpStatus.OK, "查询成功", learningAnalysisService.getTeacherDashboard(classId));
    }

    @Operation(summary = "教师基于班级真实学情向大模型提问")
    @PostMapping("/teacher/classes/{classId}/assistant")
    public Result<LearningAssistantReplyVO> askTeacherAssistant(
            @PathVariable Long classId,
            @Valid @RequestBody LearningAssistantRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "学情建议已生成", learningAnalysisService.askTeacherAssistant(classId, request));
    }

    @Operation(summary = "为真实学习行为生成AI诊断和微计划")
    @PostMapping("/teacher/cases/generate")
    public Result<LearningGrowthCaseVO> generateCase(@Valid @RequestBody LearningCaseGenerateRequest request) {
        return Result.setResult(HttpStatus.CREATED, "AI诊断计划已生成，等待教师确认", learningAnalysisService.generateCase(request));
    }

    @Operation(summary = "教师采用、编辑或拒绝AI学习计划")
    @PatchMapping("/teacher/cases/{caseId}/plan-decision")
    public Result<LearningGrowthCaseVO> decidePlan(
            @PathVariable Long caseId,
            @Valid @RequestBody LearningPlanDecisionRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "计划处理成功", learningAnalysisService.decidePlan(caseId, request));
    }

    @Operation(summary = "教师复评学生提交的学习证据")
    @PatchMapping("/teacher/plans/{planId}/review")
    public Result<LearningGrowthCaseVO> reviewPlan(
            @PathVariable Long planId,
            @Valid @RequestBody LearningPlanReviewRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "干预结论已保存", learningAnalysisService.reviewPlan(planId, request));
    }

    @Operation(summary = "学生查看AI学习成长计划")
    @GetMapping("/student/growth-overview")
    public Result<LearningStudentGrowthVO> getStudentGrowthOverview() {
        return Result.setResult(HttpStatus.OK, "查询成功", learningAnalysisService.getStudentGrowthOverview());
    }

    @Operation(summary = "基于学习画像刷新AI课程推荐")
    @PostMapping("/student/course-recommendations/refresh")
    public Result<LearningStudentGrowthVO> refreshCourseRecommendations() {
        return Result.setResult(HttpStatus.OK, "课程推荐已更新", learningAnalysisService.refreshStudentCourseRecommendations());
    }

    @Operation(summary = "学生基于本人真实学情向大模型提问")
    @PostMapping("/student/assistant")
    public Result<LearningAssistantReplyVO> askStudentAssistant(@Valid @RequestBody LearningAssistantRequest request) {
        return Result.setResult(HttpStatus.OK, "学情建议已生成", learningAnalysisService.askStudentAssistant(request));
    }

    @Operation(summary = "学生提交执行证据和理解检查回答")
    @PostMapping("/student/plans/{planId}/evidence")
    public Result<LearningGrowthCaseVO> submitEvidence(
            @PathVariable Long planId,
            @Valid @RequestBody LearningEvidenceSubmitRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "学习证据已提交，等待教师复评", learningAnalysisService.submitEvidence(planId, request));
    }
}
