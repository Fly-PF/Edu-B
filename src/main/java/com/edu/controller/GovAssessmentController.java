package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.gov.GovMockExamCreateRequest;
import com.edu.pojo.dto.gov.GovMockExamSubmitRequest;
import com.edu.pojo.vo.gov.GovMockExamRecordVO;
import com.edu.pojo.vo.gov.GovMockExamReportVO;
import com.edu.pojo.vo.gov.GovMockExamVO;
import com.edu.service.GovAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gov/assessment")
public class GovAssessmentController {
    private final GovAssessmentService govAssessmentService;

    @PostMapping("/mock")
    public Result<GovMockExamVO> createMockExam(@Valid @RequestBody GovMockExamCreateRequest request) {
        return Result.setResult(HttpStatus.CREATED, "Created", govAssessmentService.createMockExam(request));
    }

    @GetMapping("/mock/records")
    public Result<List<GovMockExamRecordVO>> listMockExamRecords() {
        return Result.setResult(HttpStatus.OK, "OK", govAssessmentService.listMockExamRecords());
    }

    @GetMapping("/mock/{practiceId}")
    public Result<GovMockExamVO> getMockExam(@PathVariable Long practiceId) {
        return Result.setResult(HttpStatus.OK, "OK", govAssessmentService.getMockExam(practiceId));
    }

    @PostMapping("/mock/{practiceId}/submit")
    public Result<GovMockExamReportVO> submitMockExam(
            @PathVariable Long practiceId,
            @RequestBody GovMockExamSubmitRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "Submitted", govAssessmentService.submitMockExam(practiceId, request));
    }

    @GetMapping("/mock/{practiceId}/report")
    public Result<GovMockExamReportVO> getMockExamReport(@PathVariable Long practiceId) {
        return Result.setResult(HttpStatus.OK, "OK", govAssessmentService.getMockExamReport(practiceId));
    }
}

