package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.course.CourseStudyRecordRequest;
import com.edu.pojo.vo.course.ChapterVO;
import com.edu.pojo.vo.course.CourseStudyRecordVO;
import com.edu.pojo.vo.course.CourseVO;
import com.edu.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
@Tag(name = "课程与学习")
public class CourseController {
    private final CourseService courseService;

    @Operation(summary = "查询平台公开课程")
    @GetMapping
    public Result<List<CourseVO>> listPublicCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Integer courseType
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "查询成功",
                courseService.listPublicCourses(keyword, grade, difficulty, courseType)
        );
    }

    @Operation(summary = "查询课程详情")
    @GetMapping("/{courseId}")
    public Result<CourseVO> getCourse(@PathVariable Long courseId) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.getCourse(courseId));
    }

    @Operation(summary = "查询课程章节与资源")
    @GetMapping("/{courseId}/chapters")
    public Result<List<ChapterVO>> listCourseChapters(@PathVariable Long courseId) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.listCourseChapters(courseId));
    }

    @Operation(summary = "查询课程学习记录")
    @GetMapping("/{courseId}/study-records")
    public Result<List<CourseStudyRecordVO>> listStudyRecords(@PathVariable Long courseId) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.listStudyRecords(courseId));
    }

    @Operation(summary = "保存课程学习记录")
    @PostMapping("/{courseId}/study-records")
    public Result<CourseStudyRecordVO> saveStudyRecord(
            @PathVariable Long courseId,
            @RequestBody CourseStudyRecordRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "保存成功", courseService.saveStudyRecord(courseId, request));
    }
}
