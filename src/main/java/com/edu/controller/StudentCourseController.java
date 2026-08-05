package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.course.CourseStudyRecordRequest;
import com.edu.pojo.vo.course.ChapterVO;
import com.edu.pojo.vo.course.CourseStudyRecordVO;
import com.edu.pojo.vo.course.CourseVO;
import com.edu.pojo.vo.course.ResourceVO;
import com.edu.pojo.dto.course.ResourceStudyRecordRequest;
import com.edu.pojo.vo.course.ResourceStudyRecordVO;
import com.edu.service.CourseResourceProgressService;
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
@RequestMapping("/api/student")
@Tag(name = "学生课程学习")
public class StudentCourseController {
    private final CourseService courseService;
    private final CourseResourceProgressService resourceProgressService;

    @Operation(summary = "学生查看课程详情")
    @GetMapping("/courses/{courseId}")
    public Result<CourseVO> getStudentCourse(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long classId
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.getCourse(courseId));
    }

    @Operation(summary = "学生查看课程章节与资源")
    @GetMapping("/courses/{courseId}/chapters")
    public Result<List<ChapterVO>> listStudentCourseChapters(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long classId
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.listCourseChapters(courseId));
    }

    @Operation(summary = "学生查看章节资源列表")
    @GetMapping("/chapters/{chapterId}/resources")
    public Result<List<ResourceVO>> listChapterResources(
            @PathVariable Long chapterId,
            @RequestParam(required = false) Long classId
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.listChapterResources(chapterId));
    }

    @Operation(summary = "学生上报章节学习进度")
    @PostMapping("/study-records")
    public Result<CourseStudyRecordVO> saveStudentStudyRecord(@RequestBody CourseStudyRecordRequest request) {
        return Result.setResult(HttpStatus.OK, "保存成功", courseService.saveStudyRecord(request));
    }

    @Operation(summary = "学生查看课程学习记录")
    @GetMapping("/courses/{courseId}/study-records")
    public Result<List<CourseStudyRecordVO>> listStudentStudyRecords(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long classId
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.listStudyRecords(courseId));
    }

    @GetMapping("/courses/{courseId}/resource-study-records")
    public Result<List<ResourceStudyRecordVO>> listResourceStudyRecords(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long assignmentId
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", resourceProgressService.listRecords(courseId, assignmentId));
    }

    @PostMapping("/resource-study-records")
    public Result<ResourceStudyRecordVO> saveResourceStudyRecord(@RequestBody ResourceStudyRecordRequest request) {
        return Result.setResult(HttpStatus.OK, "保存成功", resourceProgressService.save(request));
    }

    @PostMapping("/resource-study-records/open-block-project")
    public Result<ResourceStudyRecordVO> openBlockProject(@RequestBody ResourceStudyRecordRequest request) {
        return Result.setResult(HttpStatus.OK, "项目已打开", resourceProgressService.openBlockProject(request));
    }

    @PostMapping("/resource-study-records/complete-block-project")
    public Result<ResourceStudyRecordVO> completeBlockProject(@RequestBody ResourceStudyRecordRequest request) {
        return Result.setResult(HttpStatus.OK, "项目已完成", resourceProgressService.completeBlockProject(request));
    }

    @Operation(summary = "学生查看平台公开课程列表")
    @GetMapping("/public-courses")
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

    @Operation(summary = "学生查看平台公开课程详情")
    @GetMapping("/public-courses/{courseId}")
    public Result<CourseVO> getPublicCourse(@PathVariable Long courseId) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.getCourse(courseId));
    }
}
