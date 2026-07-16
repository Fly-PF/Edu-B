package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.mapper.EduClassMapper;
import com.edu.pojo.po.EduClassPO;
import com.edu.repository.EduClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EduClassRepositoryImpl implements EduClassRepository {
    private final EduClassMapper eduClassMapper;

    @Override
    public EduClassPO selectClassById(Long classId) {
        LambdaQueryWrapper<EduClassPO> queryWrapper = new LambdaQueryWrapper<EduClassPO>()
                .eq(EduClassPO::getId, classId);
        return eduClassMapper.selectOne(queryWrapper);
    }

    @Override
    public EduClassPO selectClassByCode(String classCode) {
        LambdaQueryWrapper<EduClassPO> queryWrapper = new LambdaQueryWrapper<EduClassPO>()
                .eq(EduClassPO::getClassCode, classCode);
        return eduClassMapper.selectOne(queryWrapper);
    }

    @Override
    public List<EduClassPO> selectClassesByTeacherId(Long teacherId) {
        LambdaQueryWrapper<EduClassPO> queryWrapper = new LambdaQueryWrapper<EduClassPO>()
                .eq(EduClassPO::getTeacherId, teacherId)
                .eq(EduClassPO::getDeleted, 0)
                .orderByDesc(EduClassPO::getCreateTime);
        return eduClassMapper.selectList(queryWrapper);
    }

    @Override
    public List<EduClassPO> selectClassesByCondition(Long teacherId, String className, String grade, Integer classStatus) {
        LambdaQueryWrapper<EduClassPO> queryWrapper = new LambdaQueryWrapper<EduClassPO>()
                .eq(EduClassPO::getTeacherId, teacherId)
                .eq(EduClassPO::getDeleted, 0)
                .like(org.springframework.util.StringUtils.hasText(className), EduClassPO::getClassName, className)
                .eq(org.springframework.util.StringUtils.hasText(grade), EduClassPO::getGrade, grade)
                .eq(classStatus != null, EduClassPO::getStatus, classStatus)
                .orderByDesc(EduClassPO::getCreateTime);
        return eduClassMapper.selectList(queryWrapper);
    }

    @Override
    public int insertClass(EduClassPO eduClassPO) {
        return eduClassMapper.insert(eduClassPO);
    }

    @Override
    public int updateClass(Long classId, String className, String grade, String school, Integer joinType, Long updateBy) {
        LambdaUpdateWrapper<EduClassPO> updateWrapper = new LambdaUpdateWrapper<EduClassPO>()
                .eq(EduClassPO::getId, classId)
                .set(EduClassPO::getClassName, className)
                .set(EduClassPO::getGrade, grade)
                .set(EduClassPO::getSchool, school)
                .set(EduClassPO::getJoinType, joinType)
                .set(EduClassPO::getUpdateBy, updateBy)
                .set(EduClassPO::getUpdateTime, LocalDateTime.now());
        return eduClassMapper.update(null, updateWrapper);
    }

    @Override
    public int updateClassStatus(Long classId, Integer status, Long updateBy) {
        LambdaUpdateWrapper<EduClassPO> updateWrapper = new LambdaUpdateWrapper<EduClassPO>()
                .eq(EduClassPO::getId, classId)
                .set(EduClassPO::getStatus, status)
                .set(EduClassPO::getUpdateBy, updateBy)
                .set(EduClassPO::getUpdateTime, LocalDateTime.now());
        return eduClassMapper.update(null, updateWrapper);
    }

    @Override
    public int deleteClass(Long classId, Long updateBy) {
        LambdaUpdateWrapper<EduClassPO> updateWrapper = new LambdaUpdateWrapper<EduClassPO>()
                .eq(EduClassPO::getId, classId)
                .set(EduClassPO::getDeleted, 1)
                .set(EduClassPO::getUpdateBy, updateBy)
                .set(EduClassPO::getUpdateTime, LocalDateTime.now());
        return eduClassMapper.update(null, updateWrapper);
    }

    @Override
    public int updateClassCode(Long classId, String classCode, Long updateBy) {
        LambdaUpdateWrapper<EduClassPO> updateWrapper = new LambdaUpdateWrapper<EduClassPO>()
                .eq(EduClassPO::getId, classId)
                .set(EduClassPO::getClassCode, classCode)
                .set(EduClassPO::getUpdateBy, updateBy)
                .set(EduClassPO::getUpdateTime, LocalDateTime.now());
        return eduClassMapper.update(null, updateWrapper);
    }

    @Override
    public int updateStudentCount(Long classId, Integer studentCount, Long updateBy) {
        LambdaUpdateWrapper<EduClassPO> updateWrapper = new LambdaUpdateWrapper<EduClassPO>()
                .eq(EduClassPO::getId, classId)
                .set(EduClassPO::getStudentCount, studentCount)
                .set(EduClassPO::getUpdateBy, updateBy)
                .set(EduClassPO::getUpdateTime, LocalDateTime.now());
        return eduClassMapper.update(null, updateWrapper);
    }
}
