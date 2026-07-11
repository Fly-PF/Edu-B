package com.edu.repository;

import com.edu.pojo.po.EduChapterPO;
import com.edu.pojo.po.EduCoursePO;
import com.edu.pojo.po.EduResourcePO;

import java.util.List;

public interface CourseModuleRepository {
    EduCoursePO selectCourseById(Long courseId);

    List<EduCoursePO> selectTeacherCourses(Long teacherId, Integer status, String keyword);

    List<EduCoursePO> selectPublishedCourses(String keyword, String grade, Integer difficulty, Integer courseType);

    int insertCourse(EduCoursePO course);

    int updateCourse(EduCoursePO course);

    int deleteCourse(Long courseId);

    EduChapterPO selectChapterById(Long chapterId);

    List<EduChapterPO> selectChaptersByCourseId(Long courseId);

    int insertChapter(EduChapterPO chapter);

    int updateChapter(EduChapterPO chapter);

    int deleteChapter(Long chapterId);

    int deleteChaptersByCourseId(Long courseId);

    EduResourcePO selectResourceById(Long resourceId);

    List<EduResourcePO> selectResourcesByChapterId(Long chapterId);

    List<EduResourcePO> selectResourcesByChapterIds(List<Long> chapterIds);

    int insertResource(EduResourcePO resource);

    int updateResource(EduResourcePO resource);

    int deleteResource(Long resourceId);

    int deleteResourcesByChapterIds(List<Long> chapterIds);

    int deleteStudyRecordsByChapterIds(List<Long> chapterIds);
}
