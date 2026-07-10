package com.edu.controller;

import com.edu.common.Result;
import com.edu.common.PageResult;
import com.edu.pojo.dto.personnel.CreatePersonnelRequest;
import com.edu.pojo.dto.personnel.UpdatePersonnelRequest;
import com.edu.pojo.dto.personnel.UpdatePersonnelStatusRequest;
import com.edu.pojo.vo.personnel.CreatePersonnelResultVO;
import com.edu.pojo.vo.personnel.PersonnelVO;
import com.edu.service.PersonnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "人员管理")
public class PersonnelController {
    private static final int STUDENT = 1;
    private static final int TEACHER = 2;
    private static final int MANAGER = 4;

    private final PersonnelService personnelService;

    @Operation(summary = "分页查询学生人员")
    @GetMapping("/students")
    public Result<PageResult<PersonnelVO>> pageStudents(@RequestParam(required = false) Integer pageNum,
                                                          @RequestParam(required = false) Integer pageSize,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Integer status) {
        return page(STUDENT, pageNum, pageSize, keyword, status);
    }

    @Operation(summary = "新增学生人员")
    @PostMapping("/students")
    public Result<Map<String, Long>> createStudent(@Valid @RequestBody CreatePersonnelRequest request) {
        return create(STUDENT, request);
    }

    @Operation(summary = "查询学生人员详情")
    @GetMapping("/students/{id}")
    public Result<PersonnelVO> getStudent(@PathVariable Long id) {
        return detail(STUDENT, id);
    }

    @Operation(summary = "编辑学生人员")
    @PatchMapping("/students/{id}")
    public Result<Void> updateStudent(@PathVariable Long id, @Valid @RequestBody UpdatePersonnelRequest request) {
        return update(STUDENT, id, request);
    }

    @Operation(summary = "启用或禁用学生人员")
    @PatchMapping("/students/{id}/status")
    public Result<Void> updateStudentStatus(@PathVariable Long id, @Valid @RequestBody UpdatePersonnelStatusRequest request) {
        return updateStatus(STUDENT, id, request);
    }

    @Operation(summary = "删除学生人员")
    @DeleteMapping("/students/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        return delete(STUDENT, id);
    }

    @Operation(summary = "分页查询教师人员")
    @GetMapping("/teachers")
    public Result<PageResult<PersonnelVO>> pageTeachers(@RequestParam(required = false) Integer pageNum,
                                                          @RequestParam(required = false) Integer pageSize,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Integer status) {
        return page(TEACHER, pageNum, pageSize, keyword, status);
    }

    @Operation(summary = "新增教师人员")
    @PostMapping("/teachers")
    public Result<Map<String, Long>> createTeacher(@Valid @RequestBody CreatePersonnelRequest request) {
        return create(TEACHER, request);
    }

    @Operation(summary = "查询教师人员详情")
    @GetMapping("/teachers/{id}")
    public Result<PersonnelVO> getTeacher(@PathVariable Long id) {
        return detail(TEACHER, id);
    }

    @Operation(summary = "编辑教师人员")
    @PatchMapping("/teachers/{id}")
    public Result<Void> updateTeacher(@PathVariable Long id, @Valid @RequestBody UpdatePersonnelRequest request) {
        return update(TEACHER, id, request);
    }

    @Operation(summary = "启用或禁用教师人员")
    @PatchMapping("/teachers/{id}/status")
    public Result<Void> updateTeacherStatus(@PathVariable Long id, @Valid @RequestBody UpdatePersonnelStatusRequest request) {
        return updateStatus(TEACHER, id, request);
    }

    @Operation(summary = "删除教师人员")
    @DeleteMapping("/teachers/{id}")
    public Result<Void> deleteTeacher(@PathVariable Long id) {
        return delete(TEACHER, id);
    }

    @Operation(summary = "分页查询管理人员")
    @GetMapping("/managers")
    public Result<PageResult<PersonnelVO>> pageManagers(@RequestParam(required = false) Integer pageNum,
                                                          @RequestParam(required = false) Integer pageSize,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Integer status) {
        return page(MANAGER, pageNum, pageSize, keyword, status);
    }

    @Operation(summary = "新增管理人员")
    @PostMapping("/managers")
    public Result<Map<String, Long>> createManager(@Valid @RequestBody CreatePersonnelRequest request) {
        return create(MANAGER, request);
    }

    @Operation(summary = "查询管理人员详情")
    @GetMapping("/managers/{id}")
    public Result<PersonnelVO> getManager(@PathVariable Long id) {
        return detail(MANAGER, id);
    }

    @Operation(summary = "编辑管理人员")
    @PatchMapping("/managers/{id}")
    public Result<Void> updateManager(@PathVariable Long id, @Valid @RequestBody UpdatePersonnelRequest request) {
        return update(MANAGER, id, request);
    }

    @Operation(summary = "启用或禁用管理人员")
    @PatchMapping("/managers/{id}/status")
    public Result<Void> updateManagerStatus(@PathVariable Long id, @Valid @RequestBody UpdatePersonnelStatusRequest request) {
        return updateStatus(MANAGER, id, request);
    }

    @Operation(summary = "删除管理人员")
    @DeleteMapping("/managers/{id}")
    public Result<Void> deleteManager(@PathVariable Long id) {
        return delete(MANAGER, id);
    }

    private Result<PageResult<PersonnelVO>> page(Integer userType, Integer pageNum, Integer pageSize, String keyword, Integer status) {
        return Result.setResult(HttpStatus.OK, "查询成功", personnelService.page(userType, pageNum, pageSize, keyword, status));
    }

    private Result<Map<String, Long>> create(Integer userType, CreatePersonnelRequest request) {
        CreatePersonnelResultVO result = personnelService.create(userType, request);
        return Result.setResult(HttpStatus.OK, result.getMessage(), Map.of("id", result.getId()));
    }

    private Result<PersonnelVO> detail(Integer userType, Long id) {
        return Result.setResult(HttpStatus.OK, "查询成功", personnelService.detail(userType, id));
    }

    private Result<Void> update(Integer userType, Long id, UpdatePersonnelRequest request) {
        personnelService.update(userType, id, request);
        return Result.setResult(HttpStatus.OK, "修改成功", null);
    }

    private Result<Void> updateStatus(Integer userType, Long id, UpdatePersonnelStatusRequest request) {
        String message = personnelService.updateStatus(userType, id, request.getStatus());
        return Result.setResult(HttpStatus.OK, message, null);
    }

    private Result<Void> delete(Integer userType, Long id) {
        personnelService.delete(userType, id);
        return Result.setResult(HttpStatus.OK, "删除成功", null);
    }
}
