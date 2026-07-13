package com.edu.service;

import com.edu.pojo.dto.course.ChapterCreateRequest;
import com.edu.pojo.dto.course.ChapterReorderItem;
import com.edu.pojo.dto.course.ChapterUpdateRequest;
import com.edu.pojo.dto.course.CourseCreateRequest;
import com.edu.pojo.dto.course.CourseStudyRecordRequest;
import com.edu.pojo.dto.course.CourseUpdateRequest;
import com.edu.pojo.dto.course.ResourceCreateRequest;
import com.edu.pojo.dto.course.ResourceUpdateRequest;
import com.edu.pojo.vo.course.ChapterVO;
import com.edu.pojo.vo.course.CourseStudyRecordVO;
import com.edu.pojo.vo.course.CourseVO;
import com.edu.pojo.vo.course.ResourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseService {
    List<CourseVO> listPublicCourses(String keyword, String grade, Integer difficulty, Integer courseType);

    CourseVO getCourse(Long courseId);

    List<ChapterVO> listCourseChapters(Long courseId);

    List<ResourceVO> listChapterResources(Long chapterId);

    List<CourseStudyRecordVO> listStudyRecords(Long courseId);

    CourseStudyRecordVO saveStudyRecord(Long courseId, CourseStudyRecordRequest request);

    CourseStudyRecordVO saveStudyRecord(CourseStudyRecordRequest request);

    List<CourseVO> listTeacherCourses(String status, String keyword);

    CourseVO createCourse(CourseCreateRequest request);

    CourseVO updateCourse(Long courseId, CourseUpdateRequest request);

    CourseVO uploadCourseCover(Long courseId, MultipartFile file);

    void deleteDraftCourse(Long courseId);

    CourseVO publishCourse(Long courseId);

    ChapterVO createChapter(Long courseId, ChapterCreateRequest request);

    ChapterVO updateChapter(Long courseId, Long chapterId, ChapterUpdateRequest request);

    void deleteChapter(Long courseId, Long chapterId);

    void reorderChapters(Long courseId, List<ChapterReorderItem> items);

    ResourceVO createResource(Long courseId, Long chapterId, ResourceCreateRequest request);

    ResourceVO uploadResource(Long courseId, Long chapterId, MultipartFile file, Integer duration);

    ResourceVO updateResource(Long courseId, Long chapterId, Long resourceId, ResourceUpdateRequest request);

    void deleteResource(Long courseId, Long chapterId, Long resourceId);

}
