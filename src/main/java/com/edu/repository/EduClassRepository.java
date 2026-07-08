package com.edu.repository;

import com.edu.pojo.po.EduClassPO;

import java.util.List;

public interface EduClassRepository {
    EduClassPO selectClassById(Long classId);

    EduClassPO selectClassByCode(String classCode);

    List<EduClassPO> selectClassesByTeacherId(Long teacherId);

    int updateClassCode(Long classId, String classCode, Long updateBy);

    int updateStudentCount(Long classId, Integer studentCount, Long updateBy);
}
