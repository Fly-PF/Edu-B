package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.vo.ai.AiFaceCompareRecordVO;
import com.edu.pojo.vo.ai.AiFaceCompareResultVO;
import com.edu.pojo.vo.ai.AiFaceProfileVO;
import com.edu.pojo.vo.ai.AiFaceRegisterResultVO;
import com.edu.service.AiFaceRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-face")
@Tag(name = "AI人脸识别体验")
public class AiFaceRecognitionController {
    private final AiFaceRecognitionService aiFaceRecognitionService;

    @Operation(summary = "查询当前用户人脸录入状态")
    @GetMapping("/profile")
    public Result<AiFaceProfileVO> getProfile() {
        return Result.setResult(HttpStatus.OK, "查询成功", aiFaceRecognitionService.getProfile());
    }

    @Operation(summary = "录入当前用户人脸")
    @PostMapping(value = "/register", consumes = {"multipart/form-data"})
    public Result<AiFaceRegisterResultVO> registerFace(@RequestPart("file") MultipartFile file) {
        return Result.setResult(HttpStatus.OK, "录入成功", aiFaceRecognitionService.registerFace(file));
    }

    @Operation(summary = "比对当前用户人脸")
    @PostMapping(value = "/compare", consumes = {"multipart/form-data"})
    public Result<AiFaceCompareResultVO> compareFace(@RequestPart("file") MultipartFile file) {
        return Result.setResult(HttpStatus.OK, "比对成功", aiFaceRecognitionService.compareFace(file));
    }

    @Operation(summary = "查询当前用户人脸比对记录")
    @GetMapping("/history")
    public Result<PageResult<AiFaceCompareRecordVO>> listHistory(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiFaceRecognitionService.listCompareHistory(pageNum, pageSize));
    }

    @Operation(summary = "清空当前用户人脸体验会话")
    @DeleteMapping("/session")
    public Result<Void> clearSession() {
        aiFaceRecognitionService.clearSession();
        return Result.setResult(HttpStatus.OK, "已清空");
    }
}
