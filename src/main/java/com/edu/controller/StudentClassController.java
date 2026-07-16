package com.edu.controller;

import com.edu.common.PageResult;
import com.edu.common.Result;
import com.edu.pojo.dto.student.StudentJoinClassRequest;
import com.edu.pojo.dto.student.StudentJoinedClassDTO;
import com.edu.pojo.dto.StudentClassCourseDTO;
import com.edu.pojo.dto.StudentClassDetailDTO;
import com.edu.service.StudentClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/classes")
@Tag(name = "学生班级")
@Tag(name = "学生班级详情")
public class StudentClassController {
    private final StudentClassService studentClassService;

    @Operation(summary = "学生加入班级")
    @PostMapping("/join")
    public Result<StudentJoinedClassDTO> joinClass(@RequestBody StudentJoinClassRequest request) {
        return Result.setResult(HttpStatus.CREATED, "加入班级成功", studentClassService.joinClass(request));
    }

    @Operation(summary = "学生退出班级")
    @DeleteMapping("/{classId}/leave")
    public Result<Void> leaveClass(@PathVariable Long classId) {
        studentClassService.leaveClass(classId);
        return Result.setResult(HttpStatus.OK, "退出班级成功");
    }

    @Operation(summary = "学生查看班级详情")
    @GetMapping("/{classId}")
    public Result<StudentClassDetailDTO> getStudentClassDetail(@PathVariable Long classId) {
        StudentClassDetailDTO detail = studentClassService.getStudentClassDetail(classId);
        return Result.setResult(HttpStatus.OK, "查询成功", detail);
    }

    @Operation(summary = "学生查看已加入班级列表")
    @GetMapping
    public Result<List<StudentJoinedClassDTO>> listJoinedClasses() {
        return Result.setResult(HttpStatus.OK, "查询成功", studentClassService.listJoinedClasses());
    }

    @Operation(summary = "学生查看班级课程列表")
    @GetMapping("/{classId}/courses")
    public Result<PageResult<StudentClassCourseDTO>> listStudentClassCourses(
            @PathVariable Long classId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer studyStatus
    ) {
        PageResult<StudentClassCourseDTO> result = studentClassService.listStudentClassCourses(
                classId, pageNum, pageSize, keyword, studyStatus
        );
        return Result.setResult(HttpStatus.OK, "查询成功", result);
    }
}
