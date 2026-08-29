package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.vo.gov.GovMaterialVO;
import com.edu.service.GovMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gov-materials")
@Tag(name = "考公网盘资料")
public class GovMaterialController {
    private final GovMaterialService materialService;

    @Operation(summary = "查询已发布的考公网盘资料")
    @GetMapping
    public Result<List<GovMaterialVO>> listPublishedMaterials(
            @RequestParam(required = false) Long categoryId
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", materialService.listPublishedMaterials(categoryId));
    }

    @Operation(summary = "读取已发布考公PDF资料")
    @GetMapping("/file")
    public ResponseEntity<byte[]> readPublishedFile(@RequestParam String fileUrl) {
        return materialService.readPublishedFile(fileUrl);
    }
}
