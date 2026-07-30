package com.edu.repository.impl.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.learning.LearningInterventionMapper;
import com.edu.pojo.po.learning.LearningInterventionPO;
import com.edu.repository.learning.LearningInterventionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LearningInterventionRepositoryImpl implements LearningInterventionRepository {
    private final LearningInterventionMapper mapper;

    @Override
    public List<LearningInterventionPO> selectByClassId(Long classId) {
        return mapper.selectList(new LambdaQueryWrapper<LearningInterventionPO>()
                .eq(LearningInterventionPO::getClassId, classId)
                .orderByDesc(LearningInterventionPO::getUpdatedAt)
                .orderByDesc(LearningInterventionPO::getId));
    }

    @Override
    public List<LearningInterventionPO> selectByStudentId(Long studentId) {
        return mapper.selectList(new LambdaQueryWrapper<LearningInterventionPO>()
                .eq(LearningInterventionPO::getStudentId, studentId)
                .orderByDesc(LearningInterventionPO::getUpdatedAt)
                .orderByDesc(LearningInterventionPO::getId));
    }

    @Override
    public LearningInterventionPO selectById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public int insert(LearningInterventionPO intervention) {
        return mapper.insert(intervention);
    }

    @Override
    public int update(LearningInterventionPO intervention) {
        return mapper.updateById(intervention);
    }
}
