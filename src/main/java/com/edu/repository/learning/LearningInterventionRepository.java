package com.edu.repository.learning;

import com.edu.pojo.po.learning.LearningInterventionPO;

import java.util.List;

public interface LearningInterventionRepository {
    List<LearningInterventionPO> selectByClassId(Long classId);

    List<LearningInterventionPO> selectByStudentId(Long studentId);

    LearningInterventionPO selectById(Long id);

    int insert(LearningInterventionPO intervention);

    int update(LearningInterventionPO intervention);
}
