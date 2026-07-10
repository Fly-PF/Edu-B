package com.edu.repository;

import com.edu.pojo.po.EduClassStudentPO;

import java.util.List;

public interface EduClassStudentRepository {
    EduClassStudentPO selectClassStudent(Long classId, Long studentId);

    List<EduClassStudentPO> selectStudentsByClassId(Long classId);

    List<EduClassStudentPO> selectClassesByStudentId(Long studentId);

    Long countStudentsByClassId(Long classId);

    int deleteClassStudent(Long classId, Long studentId);
}
