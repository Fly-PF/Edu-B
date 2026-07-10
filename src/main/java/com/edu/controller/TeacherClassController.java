package com.edu.controller;

import com.edu.common.Result;
import com.edu.common.PageResult;
import com.edu.pojo.dto.CourseDeadlineDTO;
import com.edu.pojo.dto.TeacherClassCodeDTO;
import com.edu.pojo.dto.TeacherClassCourseDTO;
import com.edu.pojo.dto.TeacherClassDetailDTO;
import com.edu.pojo.dto.TeacherClassInviteCodeDTO;
import com.edu.pojo.dto.TeacherClassStudentDTO;
import com.edu.pojo.dto.TeacherCourseStudyRecordDTO;
import com.edu.pojo.dto.TeacherStudentCourseStudyRecordDTO;
import com.edu.pojo.dto.UpdateCourseDeadlineReq;
import com.edu.pojo.dto.UpdateClassInviteCodeReq;
import com.edu.service.TeacherClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/classes")
@Tag(name = "老师班级详情")
public class TeacherClassController {
    private final TeacherClassService teacherClassService;

    @Operation(summary = "查看老师端班级详情")
    @GetMapping("/{classId}")
    public Result<TeacherClassDetailDTO> getTeacherClassDetail(@PathVariable Long classId) {
        TeacherClassDetailDTO detail = teacherClassService.getTeacherClassDetail(classId);
        return Result.setResult(HttpStatus.OK, "查询成功", detail);
    }

    @Operation(summary = "查看班级邀请码")
    @GetMapping("/{classId}/invite-code")
    public Result<TeacherClassInviteCodeDTO> getInviteCode(@PathVariable Long classId) {
        TeacherClassInviteCodeDTO inviteCode = teacherClassService.getInviteCode(classId);
        return Result.setResult(HttpStatus.OK, "查询成功", inviteCode);
    }

    @Operation(summary = "刷新班级邀请码")
    @PostMapping("/{classId}/invite-code/refresh")
    public Result<TeacherClassCodeDTO> refreshInviteCode(@PathVariable Long classId) {
        TeacherClassCodeDTO inviteCode = teacherClassService.refreshInviteCode(classId);
        return Result.setResult(HttpStatus.OK, "刷新成功", inviteCode);
    }

    @Operation(summary = "修改班级邀请码")
    @PutMapping("/{classId}/invite-code")
    public Result<TeacherClassCodeDTO> updateInviteCode(
            @PathVariable Long classId,
            @RequestBody UpdateClassInviteCodeReq req
    ) {
        TeacherClassCodeDTO inviteCode = teacherClassService.updateInviteCode(classId, req == null ? null : req.getClassCode());
        return Result.setResult(HttpStatus.OK, "修改成功", inviteCode);
    }

    @Operation(summary = "查看班级学生列表")
    @GetMapping("/{classId}/students")
    public Result<PageResult<TeacherClassStudentDTO>> listClassStudents(
            @PathVariable Long classId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword
    ) {
        PageResult<TeacherClassStudentDTO> students = teacherClassService.listClassStudents(classId, pageNum, pageSize, keyword);
        return Result.setResult(HttpStatus.OK, "查询成功", students);
    }

    @Operation(summary = "移除班级学生")
    @DeleteMapping("/{classId}/students/{studentId}")
    public Result<?> removeClassStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        teacherClassService.removeClassStudent(classId, studentId);
        return Result.setResult(HttpStatus.OK, "移除成功");
    }

    @Operation(summary = "查看班级已下发课程")
    @GetMapping("/{classId}/courses")
    public Result<PageResult<TeacherClassCourseDTO>> listAssignedCourses(
            @PathVariable Long classId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword
    ) {
        PageResult<TeacherClassCourseDTO> courses = teacherClassService.listAssignedCourses(classId, pageNum, pageSize, keyword);
        return Result.setResult(HttpStatus.OK, "查询成功", courses);
    }

    @Operation(summary = "修改班级下发课程截止时间")
    @PatchMapping("/{classId}/courses/{courseId}/deadline")
    public Result<CourseDeadlineDTO> updateCourseDeadline(
            @PathVariable Long classId,
            @PathVariable Long courseId,
            @RequestBody UpdateCourseDeadlineReq req
    ) {
        CourseDeadlineDTO deadline = teacherClassService.updateCourseDeadline(classId, courseId, req == null ? null : req.getDeadline());
        return Result.setResult(HttpStatus.OK, "修改成功", deadline);
    }

    @Operation(summary = "移除班级中的下发课程")
    @DeleteMapping("/{classId}/courses/{courseId}")
    public Result<?> removeAssignedCourse(@PathVariable Long classId, @PathVariable Long courseId) {
        teacherClassService.removeAssignedCourse(classId, courseId);
        return Result.setResult(HttpStatus.OK, "移除成功");
    }

    @Operation(summary = "查看班级课程学习进度")
    @GetMapping("/{classId}/courses/{courseId}/study-records")
    public Result<PageResult<TeacherCourseStudyRecordDTO>> listCourseStudyRecords(
            @PathVariable Long classId,
            @PathVariable Long courseId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer studyStatus
    ) {
        PageResult<TeacherCourseStudyRecordDTO> records = teacherClassService.listCourseStudyRecords(
                classId,
                courseId,
                pageNum,
                pageSize,
                keyword,
                studyStatus
        );
        return Result.setResult(HttpStatus.OK, "查询成功", records);
    }

    @Operation(summary = "查看学生课程学习明细")
    @GetMapping("/{classId}/courses/{courseId}/students/{studentId}/study-records")
    public Result<TeacherStudentCourseStudyRecordDTO> getStudentCourseStudyRecords(
            @PathVariable Long classId,
            @PathVariable Long courseId,
            @PathVariable Long studentId
    ) {
        TeacherStudentCourseStudyRecordDTO record = teacherClassService.getStudentCourseStudyRecords(
                classId,
                courseId,
                studentId
        );
        return Result.setResult(HttpStatus.OK, "查询成功", record);
    }
}
