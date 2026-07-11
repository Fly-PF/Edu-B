package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.course.ChapterCreateRequest;
import com.edu.pojo.dto.course.ChapterReorderItem;
import com.edu.pojo.dto.course.ChapterUpdateRequest;
import com.edu.pojo.dto.course.CourseCreateRequest;
import com.edu.pojo.dto.course.CourseUpdateRequest;
import com.edu.pojo.dto.course.ResourceCreateRequest;
import com.edu.pojo.dto.course.ResourceUpdateRequest;
import com.edu.pojo.vo.course.ChapterVO;
import com.edu.pojo.vo.course.CourseVO;
import com.edu.pojo.vo.course.ResourceVO;
import com.edu.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/courses")
@PreAuthorize("hasAuthority('TEACHER')")
@Tag(name = "教师课程管理")
public class TeacherCourseController {
    private final CourseService courseService;

    @Operation(summary = "查询当前教师课程")
    @GetMapping
    public Result<List<CourseVO>> listTeacherCourses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.listTeacherCourses(status, keyword));
    }

    @Operation(summary = "新建课程草稿")
    @PostMapping
    public Result<CourseVO> createCourse(@Valid @RequestBody CourseCreateRequest request) {
        return Result.setResult(HttpStatus.CREATED, "课程草稿已创建", courseService.createCourse(request));
    }

    @Operation(summary = "查询课程详情")
    @GetMapping("/{courseId}")
    public Result<CourseVO> getCourse(@PathVariable Long courseId) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.getCourse(courseId));
    }

    @Operation(summary = "修改课程信息")
    @PatchMapping("/{courseId}")
    public Result<CourseVO> updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        return Result.setResult(HttpStatus.OK, "课程已保存", courseService.updateCourse(courseId, request));
    }

    @Operation(summary = "上传课程封面")
    @PostMapping("/{courseId}/cover")
    public Result<CourseVO> uploadCourseCover(
            @PathVariable Long courseId,
            @RequestPart("file") MultipartFile file
    ) {
        return Result.setResult(HttpStatus.OK, "封面已上传", courseService.uploadCourseCover(courseId, file));
    }

    @Operation(summary = "删除课程草稿")
    @DeleteMapping("/{courseId}")
    public Result<Void> deleteDraftCourse(@PathVariable Long courseId) {
        courseService.deleteDraftCourse(courseId);
        return Result.setResult(HttpStatus.OK, "课程草稿已删除");
    }

    @Operation(summary = "发布课程")
    @PostMapping("/{courseId}/publish")
    public Result<CourseVO> publishCourse(@PathVariable Long courseId) {
        return Result.setResult(HttpStatus.OK, "课程已发布", courseService.publishCourse(courseId));
    }

    @Operation(summary = "查询课程章节资源")
    @GetMapping("/{courseId}/chapters")
    public Result<List<ChapterVO>> listChapters(@PathVariable Long courseId) {
        return Result.setResult(HttpStatus.OK, "查询成功", courseService.listCourseChapters(courseId));
    }

    @Operation(summary = "新增课程章节")
    @PostMapping("/{courseId}/chapters")
    public Result<ChapterVO> createChapter(
            @PathVariable Long courseId,
            @Valid @RequestBody ChapterCreateRequest request
    ) {
        return Result.setResult(HttpStatus.CREATED, "章节已创建", courseService.createChapter(courseId, request));
    }

    @Operation(summary = "修改课程章节")
    @PatchMapping("/{courseId}/chapters/{chapterId}")
    public Result<ChapterVO> updateChapter(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterUpdateRequest request
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "章节已保存",
                courseService.updateChapter(courseId, chapterId, request)
        );
    }

    @Operation(summary = "删除课程章节")
    @DeleteMapping("/{courseId}/chapters/{chapterId}")
    public Result<Void> deleteChapter(@PathVariable Long courseId, @PathVariable Long chapterId) {
        courseService.deleteChapter(courseId, chapterId);
        return Result.setResult(HttpStatus.OK, "章节已删除");
    }

    @Operation(summary = "调整课程章节顺序")
    @PatchMapping("/{courseId}/chapters/reorder")
    public Result<Void> reorderChapters(
            @PathVariable Long courseId,
            @Valid @RequestBody List<ChapterReorderItem> items
    ) {
        courseService.reorderChapters(courseId, items);
        return Result.setResult(HttpStatus.OK, "章节顺序已更新");
    }

    @Operation(summary = "新增外部课程资源")
    @PostMapping("/{courseId}/chapters/{chapterId}/resources")
    public Result<ResourceVO> createResource(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @Valid @RequestBody ResourceCreateRequest request
    ) {
        return Result.setResult(
                HttpStatus.CREATED,
                "资源已添加",
                courseService.createResource(courseId, chapterId, request)
        );
    }

    @Operation(summary = "上传课程资源")
    @PostMapping("/{courseId}/chapters/{chapterId}/resources/upload")
    public Result<ResourceVO> uploadResource(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Integer duration
    ) {
        return Result.setResult(
                HttpStatus.CREATED,
                "资源已上传",
                courseService.uploadResource(courseId, chapterId, file, duration)
        );
    }

    @Operation(summary = "修改课程资源")
    @PatchMapping("/{courseId}/chapters/{chapterId}/resources/{resourceId}")
    public Result<ResourceVO> updateResource(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @PathVariable Long resourceId,
            @Valid @RequestBody ResourceUpdateRequest request
    ) {
        return Result.setResult(
                HttpStatus.OK,
                "资源已保存",
                courseService.updateResource(courseId, chapterId, resourceId, request)
        );
    }

    @Operation(summary = "删除课程资源")
    @DeleteMapping("/{courseId}/chapters/{chapterId}/resources/{resourceId}")
    public Result<Void> deleteResource(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @PathVariable Long resourceId
    ) {
        courseService.deleteResource(courseId, chapterId, resourceId);
        return Result.setResult(HttpStatus.OK, "资源已删除");
    }

}
