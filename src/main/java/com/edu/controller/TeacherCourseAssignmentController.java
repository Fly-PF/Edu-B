package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.CourseAssignmentReq;
import com.edu.pojo.dto.TeacherClassCourseDTO;
import com.edu.service.TeacherClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/course-assignments")
@Tag(name = "老师课程下发")
public class TeacherCourseAssignmentController {
    private final TeacherClassService teacherClassService;

    @Operation(summary = "下发课程到班级")
    @PostMapping
    public Result<TeacherClassCourseDTO> assignCourse(@RequestBody CourseAssignmentReq req) {
        TeacherClassCourseDTO assignment = teacherClassService.assignCourse(req);
        return Result.setResult(HttpStatus.OK, "下发成功", assignment);
    }
}
