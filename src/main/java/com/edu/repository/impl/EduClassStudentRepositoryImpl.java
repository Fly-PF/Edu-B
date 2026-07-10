package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.EduClassStudentMapper;
import com.edu.pojo.po.EduClassStudentPO;
import com.edu.repository.EduClassStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EduClassStudentRepositoryImpl implements EduClassStudentRepository {
    private final EduClassStudentMapper eduClassStudentMapper;

    @Override
    public EduClassStudentPO selectClassStudent(Long classId, Long studentId) {
        LambdaQueryWrapper<EduClassStudentPO> queryWrapper = new LambdaQueryWrapper<EduClassStudentPO>()
                .eq(EduClassStudentPO::getClassId, classId)
                .eq(EduClassStudentPO::getStudentId, studentId);
        return eduClassStudentMapper.selectOne(queryWrapper);
    }

    @Override
    public List<EduClassStudentPO> selectStudentsByClassId(Long classId) {
        LambdaQueryWrapper<EduClassStudentPO> queryWrapper = new LambdaQueryWrapper<EduClassStudentPO>()
                .eq(EduClassStudentPO::getClassId, classId);
        return eduClassStudentMapper.selectList(queryWrapper);
    }

    @Override
    public List<EduClassStudentPO> selectClassesByStudentId(Long studentId) {
        LambdaQueryWrapper<EduClassStudentPO> queryWrapper = new LambdaQueryWrapper<EduClassStudentPO>()
                .eq(EduClassStudentPO::getStudentId, studentId);
        return eduClassStudentMapper.selectList(queryWrapper);
    }

    @Override
    public Long countStudentsByClassId(Long classId) {
        LambdaQueryWrapper<EduClassStudentPO> queryWrapper = new LambdaQueryWrapper<EduClassStudentPO>()
                .eq(EduClassStudentPO::getClassId, classId);
        return eduClassStudentMapper.selectCount(queryWrapper);
    }

    @Override
    public int deleteClassStudent(Long classId, Long studentId) {
        LambdaQueryWrapper<EduClassStudentPO> queryWrapper = new LambdaQueryWrapper<EduClassStudentPO>()
                .eq(EduClassStudentPO::getClassId, classId)
                .eq(EduClassStudentPO::getStudentId, studentId);
        return eduClassStudentMapper.delete(queryWrapper);
    }
}
