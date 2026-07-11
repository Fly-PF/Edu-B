package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.EduChapterMapper;
import com.edu.mapper.EduCourseMapper;
import com.edu.mapper.EduResourceMapper;
import com.edu.mapper.EduStudyRecordMapper;
import com.edu.pojo.po.EduChapterPO;
import com.edu.pojo.po.EduCoursePO;
import com.edu.pojo.po.EduResourcePO;
import com.edu.pojo.po.EduStudyRecordPO;
import com.edu.repository.CourseModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseModuleRepositoryImpl implements CourseModuleRepository {
    private final EduCourseMapper courseMapper;
    private final EduChapterMapper chapterMapper;
    private final EduResourceMapper resourceMapper;
    private final EduStudyRecordMapper studyRecordMapper;

    @Override
    public EduCoursePO selectCourseById(Long courseId) {
        return courseMapper.selectOne(new LambdaQueryWrapper<EduCoursePO>()
                .eq(EduCoursePO::getId, courseId)
                .eq(EduCoursePO::getDeleted, 0));
    }

    @Override
    public List<EduCoursePO> selectTeacherCourses(Long teacherId, Integer status, String keyword) {
        return courseMapper.selectList(new LambdaQueryWrapper<EduCoursePO>()
                .eq(EduCoursePO::getTeacherId, teacherId)
                .eq(EduCoursePO::getDeleted, 0)
                .eq(status != null, EduCoursePO::getStatus, status)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(EduCoursePO::getCourseName, keyword)
                        .or()
                        .like(EduCoursePO::getIntro, keyword))
                .orderByDesc(EduCoursePO::getUpdateTime)
                .orderByDesc(EduCoursePO::getId));
    }

    @Override
    public List<EduCoursePO> selectPublishedCourses(
            String keyword,
            String grade,
            Integer difficulty,
            Integer courseType
    ) {
        return courseMapper.selectList(new LambdaQueryWrapper<EduCoursePO>()
                .eq(EduCoursePO::getStatus, 1)
                .eq(EduCoursePO::getPublicFlag, 1)
                .eq(EduCoursePO::getDeleted, 0)
                .eq(StringUtils.hasText(grade), EduCoursePO::getGrade, grade)
                .eq(difficulty != null, EduCoursePO::getDifficulty, difficulty)
                .eq(courseType != null, EduCoursePO::getCourseType, courseType)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(EduCoursePO::getCourseName, keyword)
                        .or()
                        .like(EduCoursePO::getIntro, keyword))
                .orderByDesc(EduCoursePO::getUpdateTime)
                .orderByDesc(EduCoursePO::getId));
    }

    @Override
    public int insertCourse(EduCoursePO course) {
        return courseMapper.insert(course);
    }

    @Override
    public int updateCourse(EduCoursePO course) {
        return courseMapper.updateById(course);
    }

    @Override
    public int deleteCourse(Long courseId) {
        return courseMapper.deleteById(courseId);
    }

    @Override
    public EduChapterPO selectChapterById(Long chapterId) {
        return chapterMapper.selectOne(new LambdaQueryWrapper<EduChapterPO>()
                .eq(EduChapterPO::getId, chapterId)
                .eq(EduChapterPO::getDeleted, 0));
    }

    @Override
    public List<EduChapterPO> selectChaptersByCourseId(Long courseId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<EduChapterPO>()
                .eq(EduChapterPO::getCourseId, courseId)
                .eq(EduChapterPO::getDeleted, 0)
                .orderByAsc(EduChapterPO::getSort)
                .orderByAsc(EduChapterPO::getId));
    }

    @Override
    public int insertChapter(EduChapterPO chapter) {
        return chapterMapper.insert(chapter);
    }

    @Override
    public int updateChapter(EduChapterPO chapter) {
        return chapterMapper.updateById(chapter);
    }

    @Override
    public int deleteChapter(Long chapterId) {
        return chapterMapper.deleteById(chapterId);
    }

    @Override
    public int deleteChaptersByCourseId(Long courseId) {
        return chapterMapper.delete(new LambdaQueryWrapper<EduChapterPO>()
                .eq(EduChapterPO::getCourseId, courseId));
    }

    @Override
    public EduResourcePO selectResourceById(Long resourceId) {
        return resourceMapper.selectOne(new LambdaQueryWrapper<EduResourcePO>()
                .eq(EduResourcePO::getId, resourceId)
                .eq(EduResourcePO::getDeleted, 0));
    }

    @Override
    public List<EduResourcePO> selectResourcesByChapterId(Long chapterId) {
        return resourceMapper.selectList(new LambdaQueryWrapper<EduResourcePO>()
                .eq(EduResourcePO::getChapterId, chapterId)
                .eq(EduResourcePO::getDeleted, 0)
                .orderByAsc(EduResourcePO::getSort)
                .orderByAsc(EduResourcePO::getId));
    }

    @Override
    public List<EduResourcePO> selectResourcesByChapterIds(List<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return List.of();
        }
        return resourceMapper.selectList(new LambdaQueryWrapper<EduResourcePO>()
                .in(EduResourcePO::getChapterId, chapterIds)
                .eq(EduResourcePO::getDeleted, 0)
                .orderByAsc(EduResourcePO::getSort)
                .orderByAsc(EduResourcePO::getId));
    }

    @Override
    public int insertResource(EduResourcePO resource) {
        return resourceMapper.insert(resource);
    }

    @Override
    public int updateResource(EduResourcePO resource) {
        return resourceMapper.updateById(resource);
    }

    @Override
    public int deleteResource(Long resourceId) {
        return resourceMapper.deleteById(resourceId);
    }

    @Override
    public int deleteResourcesByChapterIds(List<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return 0;
        }
        return resourceMapper.delete(new LambdaQueryWrapper<EduResourcePO>()
                .in(EduResourcePO::getChapterId, chapterIds));
    }

    @Override
    public int deleteStudyRecordsByChapterIds(List<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return 0;
        }
        return studyRecordMapper.delete(new LambdaQueryWrapper<EduStudyRecordPO>()
                .in(EduStudyRecordPO::getChapterId, chapterIds));
    }

}
