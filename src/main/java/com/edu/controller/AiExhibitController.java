package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.ai.AiDrawGuessRequest;
import com.edu.pojo.vo.ai.AiDrawGuessResultVO;
import com.edu.pojo.vo.ai.AiExhibitOverviewVO;
import com.edu.pojo.vo.ai.AiPracticeRecordVO;
import com.edu.pojo.vo.ai.AiProjectCaseVO;
import com.edu.service.AiDrawGuessService;
import com.edu.service.AiExhibitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-exhibit")
@Tag(name = "AI展馆")
public class AiExhibitController {
    private final AiExhibitService aiExhibitService;
    private final AiDrawGuessService aiDrawGuessService;

    @Operation(summary = "查询 AI 展馆概览")
    @GetMapping("/overview")
    public Result<AiExhibitOverviewVO> getOverview() {
        return Result.setResult(HttpStatus.OK, "查询成功", aiExhibitService.getOverview());
    }

    @Operation(summary = "分页查询 AI 项目案例")
    @GetMapping("/cases")
    public Result<PageResult<AiProjectCaseVO>> listCases(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gradeBand,
            @RequestParam(required = false) String subjectDirection,
            @RequestParam(required = false) String practiceType
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "查询成功",
                aiExhibitService.listCases(pageNum, pageSize, keyword, gradeBand, subjectDirection, practiceType)
        );
    }

    @Operation(summary = "查询 AI 项目案例详情")
    @GetMapping("/cases/{caseId}")
    public Result<AiProjectCaseVO> getCase(@PathVariable Long caseId) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiExhibitService.getCase(caseId));
    }

    @Operation(summary = "查询我的实践记录")
    @GetMapping("/records")
    public Result<PageResult<AiPracticeRecordVO>> listMyRecords(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long caseId
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiExhibitService.listMyRecords(pageNum, pageSize, caseId));
    }

    @Operation(summary = "提交 AI 项目实践")
    @PostMapping(value = "/cases/{caseId}/records", consumes = {"multipart/form-data"})
    public Result<AiPracticeRecordVO> submitPractice(
            @PathVariable Long caseId,
            @RequestParam(required = false) String practiceType,
            @RequestParam(required = false) String inputText,
            @RequestParam(required = false) String answerText,
            @RequestParam(required = false) String note,
            @RequestPart(required = false) MultipartFile file
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "提交成功",
                aiExhibitService.submitPractice(caseId, practiceType, inputText, answerText, note, file)
        );
    }

    @Operation(summary = "你画我猜 AI 识别")
    @PostMapping("/draw-guess")
    public Result<AiDrawGuessResultVO> drawGuess(@Valid @RequestBody AiDrawGuessRequest request) {
        return Result.setResult(HttpStatus.OK, "识别成功", aiDrawGuessService.guess(request));
    }
}
