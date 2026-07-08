package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.EduCourseMapper;
import com.edu.pojo.po.EduCoursePO;
import com.edu.repository.EduCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EduCourseRepositoryImpl implements EduCourseRepository {
    private final EduCourseMapper eduCourseMapper;

    @Override
    public EduCoursePO selectCourseById(Long courseId) {
        LambdaQueryWrapper<EduCoursePO> queryWrapper = new LambdaQueryWrapper<EduCoursePO>()
                .eq(EduCoursePO::getId, courseId);
        return eduCourseMapper.selectOne(queryWrapper);
    }

    @Override
    public List<EduCoursePO> selectCoursesByTeacherId(Long teacherId) {
        LambdaQueryWrapper<EduCoursePO> queryWrapper = new LambdaQueryWrapper<EduCoursePO>()
                .eq(EduCoursePO::getTeacherId, teacherId);
        return eduCourseMapper.selectList(queryWrapper);
    }

    @Override
    public List<EduCoursePO> selectPublicCourses() {
        LambdaQueryWrapper<EduCoursePO> queryWrapper = new LambdaQueryWrapper<EduCoursePO>()
                .eq(EduCoursePO::getPublicFlag, 1);
        return eduCourseMapper.selectList(queryWrapper);
    }
}
