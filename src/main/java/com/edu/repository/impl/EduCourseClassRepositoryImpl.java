package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.EduCourseClassMapper;
import com.edu.pojo.po.EduCourseClassPO;
import com.edu.repository.EduCourseClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EduCourseClassRepositoryImpl implements EduCourseClassRepository {
    private final EduCourseClassMapper eduCourseClassMapper;

    @Override
    public EduCourseClassPO selectCourseClass(Long courseId, Long classId) {
        LambdaQueryWrapper<EduCourseClassPO> queryWrapper = new LambdaQueryWrapper<EduCourseClassPO>()
                .eq(EduCourseClassPO::getCourseId, courseId)
                .eq(EduCourseClassPO::getClassId, classId);
        return eduCourseClassMapper.selectOne(queryWrapper);
    }

    @Override
    public List<EduCourseClassPO> selectByCourseId(Long courseId) {
        LambdaQueryWrapper<EduCourseClassPO> queryWrapper = new LambdaQueryWrapper<EduCourseClassPO>()
                .eq(EduCourseClassPO::getCourseId, courseId);
        return eduCourseClassMapper.selectList(queryWrapper);
    }

    @Override
    public List<EduCourseClassPO> selectByClassId(Long classId) {
        LambdaQueryWrapper<EduCourseClassPO> queryWrapper = new LambdaQueryWrapper<EduCourseClassPO>()
                .eq(EduCourseClassPO::getClassId, classId);
        return eduCourseClassMapper.selectList(queryWrapper);
    }

    @Override
    public int insertCourseClass(EduCourseClassPO eduCourseClassPO) {
        return eduCourseClassMapper.insert(eduCourseClassPO);
    }

    @Override
    public int updateDeadline(Long courseId, Long classId, LocalDateTime deadline) {
        LambdaUpdateWrapper<EduCourseClassPO> updateWrapper = new LambdaUpdateWrapper<EduCourseClassPO>()
                .eq(EduCourseClassPO::getCourseId, courseId)
                .eq(EduCourseClassPO::getClassId, classId)
                .set(EduCourseClassPO::getDeadline, deadline);
        return eduCourseClassMapper.update(null, updateWrapper);
    }

    @Override
    public int deleteCourseClass(Long courseId, Long classId) {
        LambdaQueryWrapper<EduCourseClassPO> queryWrapper = new LambdaQueryWrapper<EduCourseClassPO>()
                .eq(EduCourseClassPO::getCourseId, courseId)
                .eq(EduCourseClassPO::getClassId, classId);
        return eduCourseClassMapper.delete(queryWrapper);
    }
}
