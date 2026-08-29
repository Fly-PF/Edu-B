package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.gov.GovGoalSaveRequest;
import com.edu.pojo.dto.gov.GovPlanTaskSaveRequest;
import com.edu.pojo.vo.gov.GovGoalVO;
import com.edu.pojo.vo.gov.GovPlanTaskVO;
import com.edu.service.GovGoalPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gov/goal-plan")
@Tag(name = "考公目标与学习计划")
public class GovGoalPlanController {
    private final GovGoalPlanService service;

    @GetMapping("/goal")
    @Operation(summary = "获取当前公考目标")
    public Result<GovGoalVO> getGoal() { return Result.setResult(HttpStatus.OK, "查询成功", service.getGoal()); }

    @PutMapping("/goal")
    @Operation(summary = "保存当前公考目标")
    public Result<GovGoalVO> saveGoal(@Valid @RequestBody GovGoalSaveRequest request) { return Result.setResult(HttpStatus.OK, "目标已保存", service.saveGoal(request)); }

    @GetMapping("/tasks")
    @Operation(summary = "查询学习便签")
    public Result<List<GovPlanTaskVO>> listTasks(@RequestParam(required = false) LocalDate taskDate) { return Result.setResult(HttpStatus.OK, "查询成功", service.listTasks(taskDate)); }

    @PostMapping("/tasks")
    @Operation(summary = "新增学习便签")
    public Result<GovPlanTaskVO> createTask(@Valid @RequestBody GovPlanTaskSaveRequest request) { return Result.setResult(HttpStatus.CREATED, "任务已创建", service.createTask(request)); }

    @PutMapping("/tasks/{taskId}")
    @Operation(summary = "编辑学习便签")
    public Result<GovPlanTaskVO> updateTask(@PathVariable Long taskId, @Valid @RequestBody GovPlanTaskSaveRequest request) { return Result.setResult(HttpStatus.OK, "任务已保存", service.updateTask(taskId, request)); }

    @PatchMapping("/tasks/{taskId}/complete")
    @Operation(summary = "完成或取消完成学习便签")
    public Result<GovPlanTaskVO> toggleTask(@PathVariable Long taskId, @RequestParam boolean completed) { return Result.setResult(HttpStatus.OK, completed ? "任务已完成" : "已取消完成", service.toggleTask(taskId, completed)); }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "删除学习便签")
    public Result<Void> deleteTask(@PathVariable Long taskId) { service.deleteTask(taskId); return Result.setResult(HttpStatus.OK, "任务已删除"); }
}
