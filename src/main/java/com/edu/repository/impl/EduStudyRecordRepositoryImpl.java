package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.EduStudyRecordMapper;
import com.edu.pojo.po.EduStudyRecordPO;
import com.edu.repository.EduStudyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EduStudyRecordRepositoryImpl implements EduStudyRecordRepository {
    private final EduStudyRecordMapper eduStudyRecordMapper;

    @Override
    public EduStudyRecordPO selectStudyRecord(Long studentId, Long chapterId) {
        LambdaQueryWrapper<EduStudyRecordPO> queryWrapper = new LambdaQueryWrapper<EduStudyRecordPO>()
                .eq(EduStudyRecordPO::getStudentId, studentId)
                .eq(EduStudyRecordPO::getChapterId, chapterId);
        return eduStudyRecordMapper.selectOne(queryWrapper);
    }

    @Override
    public List<EduStudyRecordPO> selectRecordsByStudentId(Long studentId) {
        LambdaQueryWrapper<EduStudyRecordPO> queryWrapper = new LambdaQueryWrapper<EduStudyRecordPO>()
                .eq(EduStudyRecordPO::getStudentId, studentId);
        return eduStudyRecordMapper.selectList(queryWrapper);
    }

    @Override
    public List<EduStudyRecordPO> selectRecordsByCourseId(Long courseId) {
        LambdaQueryWrapper<EduStudyRecordPO> queryWrapper = new LambdaQueryWrapper<EduStudyRecordPO>()
                .eq(EduStudyRecordPO::getCourseId, courseId);
        return eduStudyRecordMapper.selectList(queryWrapper);
    }

    @Override
    public int insertStudyRecord(EduStudyRecordPO record) {
        return eduStudyRecordMapper.insert(record);
    }

    @Override
    public int updateStudyRecord(EduStudyRecordPO record) {
        return eduStudyRecordMapper.updateById(record);
    }
}
