package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.safety.RagEvidenceRequest;
import com.edu.pojo.dto.safety.RagEvidenceResponse;
import com.edu.pojo.dto.safety.SafetyDashboardDTO;
import com.edu.pojo.dto.safety.SafetyEvaluationResultDTO;
import com.edu.pojo.dto.safety.SafetyEvaluationRunRequest;
import com.edu.pojo.dto.safety.SafetyGatewayRequest;
import com.edu.pojo.dto.safety.SafetyGatewayResponse;
import com.edu.pojo.dto.safety.SafetyRecordDTO;
import com.edu.pojo.dto.safety.SafetyReviewActionRequest;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.pojo.enums.safety.SafetyReviewStatus;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import com.edu.service.safety.RagEvidenceService;
import com.edu.service.safety.SafetyEvaluationService;
import com.edu.service.safety.SafetyGatewayService;
import com.edu.service.safety.SafetyRecordService;
import com.edu.util.SecurityUtil;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/safety")
@Tag(name = "AI教育安全评测中心")
public class SafetyController {
    static final String SAFETY_GATEWAY_AUTHORITY = "hasAnyAuthority('ADMIN','SUPERADMIN','TEACHER','STUDENT')";
    static final String SAFETY_STAFF_AUTHORITY = "hasAnyAuthority('ADMIN','SUPERADMIN','TEACHER')";
    static final String SAFETY_ADMIN_AUTHORITY = "hasAnyAuthority('ADMIN','SUPERADMIN')";

    private final SafetyGatewayService safetyGatewayService;
    private final RagEvidenceService ragEvidenceService;
    private final SafetyRecordService safetyRecordService;
    private final SafetyEvaluationService safetyEvaluationService;

    @Operation(summary = "统一安全检测")
    @PostMapping("/check")
    @PreAuthorize(SAFETY_STAFF_AUTHORITY)
    public Result<SafetyGatewayResponse> check(@Valid @RequestBody SafetyGatewayRequest request) {
        return Result.setResult(HttpStatus.OK, "检测完成", safetyGatewayService.evaluate(request));
    }

    @Operation(summary = "统一安全网关")
    @PostMapping("/gateway")
    @PreAuthorize(SAFETY_GATEWAY_AUTHORITY)
    public Result<SafetyGatewayResponse> gateway(@Valid @RequestBody SafetyGatewayRequest request) {
        SafetyGatewayRequest normalizedRequest = normalizeGatewayRequest(request);
        return Result.setResult(HttpStatus.OK, "网关处理完成", safetyGatewayService.evaluate(normalizedRequest));
    }

    @Operation(summary = "教育RAG证据校验")
    @PostMapping("/evidence/check")
    @PreAuthorize(SAFETY_STAFF_AUTHORITY)
    public Result<RagEvidenceResponse> checkEvidence(@RequestBody RagEvidenceRequest request) {
        return Result.setResult(HttpStatus.OK, "证据校验完成", ragEvidenceService.checkEvidence(request));
    }

    @Operation(summary = "安全评测样本批跑")
    @PostMapping("/evaluation/run")
    @PreAuthorize(SAFETY_ADMIN_AUTHORITY)
    public Result<SafetyEvaluationResultDTO> runEvaluation(@Valid @RequestBody SafetyEvaluationRunRequest request) {
        return Result.setResult(HttpStatus.OK, "评测完成", safetyEvaluationService.runEvaluation(request));
    }

    @Operation(summary = "安全态势看板")
    @GetMapping("/dashboard")
    @PreAuthorize(SAFETY_ADMIN_AUTHORITY)
    public Result<SafetyDashboardDTO> dashboard(@RequestParam(required = false) Integer days) {
        return Result.setResult(HttpStatus.OK, "查询成功", safetyRecordService.dashboard(days));
    }

    @Operation(summary = "安全检测记录列表")
    @GetMapping("/records")
    @PreAuthorize(SAFETY_ADMIN_AUTHORITY)
    public Result<PageResult<SafetyRecordDTO>> pageRecords(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) SafetySourceModule sourceModule,
            @RequestParam(required = false) SafetyScene scene,
            @RequestParam(required = false) SafetyUserRole userRole,
            @RequestParam(required = false) SafetyGradeLevel gradeLevel,
            @RequestParam(required = false) SafetyRiskLevel riskLevel,
            @RequestParam(required = false) SafetyRiskType riskType,
            @RequestParam(required = false) SafetyDecision decision,
            @RequestParam(required = false) SafetyReviewStatus reviewStatus,
            @RequestParam(required = false) Boolean manualReviewRequired,
            @RequestParam(required = false) String keyword
    ) {
        PageResult<SafetyRecordDTO> result = safetyRecordService.pageRecords(
                pageNum,
                pageSize,
                sourceModule,
                scene,
                userRole,
                gradeLevel,
                riskLevel,
                riskType,
                decision,
                reviewStatus,
                manualReviewRequired,
                keyword
        );
        return Result.setResult(HttpStatus.OK, "查询成功", result);
    }

    @Operation(summary = "安全检测记录详情")
    @GetMapping("/records/{id}")
    @PreAuthorize(SAFETY_ADMIN_AUTHORITY)
    public Result<SafetyRecordDTO> recordDetail(@PathVariable Long id) {
        return Result.setResult(HttpStatus.OK, "查询成功", safetyRecordService.detail(id));
    }

    @Operation(summary = "安全检测记录审核通过")
    @PostMapping("/records/{id}/approve")
    @PreAuthorize(SAFETY_ADMIN_AUTHORITY)
    public Result<SafetyRecordDTO> approveRecordReview(@PathVariable Long id,
                                                       @RequestBody(required = false) SafetyReviewActionRequest request) {
        return Result.setResult(HttpStatus.OK, "审核通过成功", safetyRecordService.approveReview(id, request));
    }

    @Operation(summary = "安全检测记录审核驳回")
    @PostMapping("/records/{id}/reject")
    @PreAuthorize(SAFETY_ADMIN_AUTHORITY)
    public Result<SafetyRecordDTO> rejectRecordReview(@PathVariable Long id,
                                                      @RequestBody(required = false) SafetyReviewActionRequest request) {
        return Result.setResult(HttpStatus.OK, "审核驳回成功", safetyRecordService.rejectReview(id, request));
    }

    @Operation(summary = "审核队列")
    @GetMapping("/reviews")
    @PreAuthorize(SAFETY_STAFF_AUTHORITY)
    public Result<PageResult<SafetyRecordDTO>> pageReviewRecords(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) SafetySourceModule sourceModule,
            @RequestParam(required = false) SafetyScene scene,
            @RequestParam(required = false) SafetyUserRole userRole,
            @RequestParam(required = false) SafetyGradeLevel gradeLevel,
            @RequestParam(required = false) SafetyRiskLevel riskLevel,
            @RequestParam(required = false) SafetyRiskType riskType,
            @RequestParam(required = false) SafetyDecision decision,
            @RequestParam(required = false) SafetyReviewStatus reviewStatus,
            @RequestParam(required = false) Boolean manualReviewRequired,
            @RequestParam(required = false) String keyword
    ) {
        PageResult<SafetyRecordDTO> result = safetyRecordService.pageReviewRecords(
                pageNum,
                pageSize,
                classId,
                sourceModule,
                scene,
                userRole,
                gradeLevel,
                riskLevel,
                riskType,
                decision,
                reviewStatus,
                manualReviewRequired,
                keyword
        );
        return Result.setResult(HttpStatus.OK, "查询成功", result);
    }

    @Operation(summary = "审核详情")
    @GetMapping("/reviews/{id}")
    @PreAuthorize(SAFETY_STAFF_AUTHORITY)
    public Result<SafetyRecordDTO> reviewDetail(@PathVariable Long id) {
        return Result.setResult(HttpStatus.OK, "查询成功", safetyRecordService.reviewDetail(id));
    }

    @Operation(summary = "审核通过")
    @PostMapping("/reviews/{id}/approve")
    @PreAuthorize(SAFETY_STAFF_AUTHORITY)
    public Result<SafetyRecordDTO> approveReview(@PathVariable Long id,
                                                 @RequestBody(required = false) SafetyReviewActionRequest request) {
        return Result.setResult(HttpStatus.OK, "审核通过成功", safetyRecordService.approveReview(id, request));
    }

    @Operation(summary = "审核驳回")
    @PostMapping("/reviews/{id}/reject")
    @PreAuthorize(SAFETY_STAFF_AUTHORITY)
    public Result<SafetyRecordDTO> rejectReview(@PathVariable Long id,
                                                @RequestBody(required = false) SafetyReviewActionRequest request) {
        return Result.setResult(HttpStatus.OK, "审核驳回成功", safetyRecordService.rejectReview(id, request));
    }

    @Operation(summary = "审核决策兼容接口")
    @PostMapping("/reviews/{id}/decision")
    @PreAuthorize(SAFETY_STAFF_AUTHORITY)
    public Result<SafetyRecordDTO> reviewDecision(@PathVariable Long id,
                                                  @RequestBody(required = false) SafetyReviewActionRequest request) {
        SafetyReviewStatus reviewStatus = request == null ? null : request.getReviewStatus();
        if (reviewStatus == SafetyReviewStatus.REJECTED) {
            return Result.setResult(HttpStatus.OK, "审核驳回成功", safetyRecordService.rejectReview(id, request));
        }
        return Result.setResult(HttpStatus.OK, "审核通过成功", safetyRecordService.approveReview(id, request));
    }

    private SafetyGatewayRequest normalizeGatewayRequest(SafetyGatewayRequest request) {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null) {
            return request;
        }
        request.setUserId(loginUser.getUserId());
        request.setUserRole(resolveGatewayUserRole(loginUser.getRoleCode()));
        return request;
    }

    private SafetyUserRole resolveGatewayUserRole(String roleCode) {
        if ("STUDENT".equalsIgnoreCase(roleCode)) {
            return SafetyUserRole.STUDENT;
        }
        if ("TEACHER".equalsIgnoreCase(roleCode)) {
            return SafetyUserRole.TEACHER;
        }
        return SafetyUserRole.ADMIN;
    }
}
