package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.skill.AiSkillInvokeRequest;
import com.edu.pojo.dto.skill.AiSkillSaveRequest;
import com.edu.pojo.vo.skill.AiSkillCategoryVO;
import com.edu.pojo.vo.skill.AiSkillInvokeVO;
import com.edu.pojo.vo.skill.AiSkillVO;
import com.edu.service.AiSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-skills")
@Tag(name = "AI Skill 技能市场")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','TEACHER','STUDENT')")
public class AiSkillController {
    private final AiSkillService aiSkillService;

    @Operation(summary = "查询可用Skill分类")
    @GetMapping("/categories")
    public Result<List<AiSkillCategoryVO>> listCategories(@RequestParam(required = false) String categoryType) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiSkillService.listCategories(categoryType, true));
    }

    @Operation(summary = "Skill市场分页")
    @GetMapping("/market")
    public Result<PageResult<AiSkillVO>> pageMarket(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String skillType,
                                                    @RequestParam(required = false) String subjectType,
                                                    @RequestParam(required = false) String targetAudience,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.setResult(HttpStatus.OK, "查询成功",
                aiSkillService.pageMarket(keyword, skillType, subjectType, targetAudience, categoryId, pageNum, pageSize));
    }

    @Operation(summary = "我的Skill分页")
    @GetMapping("/mine")
    public Result<PageResult<AiSkillVO>> pageMySkills(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiSkillService.pageMySkills(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "我的收藏Skill")
    @GetMapping("/collections")
    public Result<PageResult<AiSkillVO>> pageCollections(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiSkillService.pageCollections(pageNum, pageSize));
    }

    @Operation(summary = "Skill详情")
    @GetMapping("/{skillId}")
    public Result<AiSkillVO> detail(@PathVariable Long skillId) {
        return Result.setResult(HttpStatus.OK, "查询成功", aiSkillService.detail(skillId));
    }

    @Operation(summary = "创建Skill草稿")
    @PostMapping
    public Result<AiSkillVO> createDraft(@RequestBody AiSkillSaveRequest request) {
        return Result.setResult(HttpStatus.CREATED, "保存成功", aiSkillService.createDraft(request));
    }

    @Operation(summary = "编辑保存Skill")
    @PutMapping("/{skillId}")
    public Result<AiSkillVO> updateSkill(@PathVariable Long skillId, @RequestBody AiSkillSaveRequest request) {
        return Result.setResult(HttpStatus.OK, "保存成功", aiSkillService.updateSkill(skillId, request));
    }

    @Operation(summary = "发布Skill")
    @PostMapping("/{skillId}/publish")
    public Result<AiSkillVO> publish(@PathVariable Long skillId) {
        return Result.setResult(HttpStatus.OK, "发布成功", aiSkillService.publish(skillId));
    }

    @Operation(summary = "下线Skill")
    @PostMapping("/{skillId}/offline")
    public Result<AiSkillVO> offline(@PathVariable Long skillId) {
        return Result.setResult(HttpStatus.OK, "下线成功", aiSkillService.offline(skillId));
    }

    @Operation(summary = "删除Skill")
    @DeleteMapping("/{skillId}")
    public Result<Void> deleteSkill(@PathVariable Long skillId) {
        aiSkillService.deleteSkill(skillId);
        return Result.setResult(HttpStatus.OK, "删除成功");
    }

    @Operation(summary = "收藏Skill")
    @PostMapping("/{skillId}/collect")
    public Result<Void> collect(@PathVariable Long skillId) {
        aiSkillService.collect(skillId);
        return Result.setResult(HttpStatus.OK, "收藏成功");
    }

    @Operation(summary = "取消收藏Skill")
    @DeleteMapping("/{skillId}/collect")
    public Result<Void> cancelCollect(@PathVariable Long skillId) {
        aiSkillService.cancelCollect(skillId);
        return Result.setResult(HttpStatus.OK, "取消成功");
    }

    @Operation(summary = "导入外部Skill zip")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AiSkillVO> importZip(@RequestPart("file") MultipartFile file,
                                       @RequestParam(required = false) String skillName,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(required = false) String skillType,
                                       @RequestParam(required = false) String subjectType,
                                       @RequestParam(required = false) String targetAudience,
                                       @RequestParam(required = false) String visibility,
                                       @RequestParam(required = false) List<Long> categoryIds) {
        return Result.setResult(HttpStatus.CREATED, "导入成功",
                aiSkillService.importZip(file, skillName, description, skillType, subjectType, targetAudience, visibility, categoryIds));
    }

    @Operation(summary = "导出或市场下载Skill")
    @GetMapping("/{skillId}/export")
    public ResponseEntity<byte[]> exportSkill(@PathVariable Long skillId) {
        return aiSkillService.exportSkill(skillId);
    }

    @Operation(summary = "调用Skill并生成提示词")
    @PostMapping("/{skillId}/invoke")
    public Result<AiSkillInvokeVO> invokeSkill(@PathVariable Long skillId, @RequestBody AiSkillInvokeRequest request) {
        return Result.setResult(HttpStatus.OK, "调用成功", aiSkillService.invokeSkill(skillId, request));
    }
}
