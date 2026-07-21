package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.block.BlockProjectSaveRequest;
import com.edu.pojo.vo.block.BlockProjectVO;
import com.edu.service.BlockProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/block-projects")
public class BlockProjectController {
    private final BlockProjectService blockProjectService;

    @GetMapping("/mine")
    public Result<List<BlockProjectVO>> listMine() {
        return Result.setResult(HttpStatus.OK, "OK", blockProjectService.listMine());
    }

    @GetMapping("/gallery")
    public Result<List<BlockProjectVO>> listGallery(@RequestParam(required = false) String keyword) {
        return Result.setResult(HttpStatus.OK, "OK", blockProjectService.listGallery(keyword));
    }

    @GetMapping("/{projectId}")
    public Result<BlockProjectVO> getProject(@PathVariable Long projectId) {
        return Result.setResult(HttpStatus.OK, "OK", blockProjectService.getProject(projectId));
    }

    @PostMapping
    public Result<BlockProjectVO> createProject(@Valid @RequestBody BlockProjectSaveRequest request) {
        return Result.setResult(HttpStatus.CREATED, "Created", blockProjectService.createProject(request));
    }

    @PutMapping("/{projectId}")
    public Result<BlockProjectVO> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody BlockProjectSaveRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "Saved", blockProjectService.updateProject(projectId, request));
    }

    @PostMapping("/{projectId}/publish")
    public Result<BlockProjectVO> publishProject(@PathVariable Long projectId) {
        return Result.setResult(HttpStatus.OK, "Published", blockProjectService.publishProject(projectId));
    }

    @PostMapping("/{projectId}/remix")
    public Result<BlockProjectVO> remixProject(@PathVariable Long projectId) {
        return Result.setResult(HttpStatus.CREATED, "Created", blockProjectService.remixProject(projectId));
    }
}
