package com.edu.repository;

import com.edu.pojo.po.EduClassPO;

import java.util.List;

public interface EduClassRepository {
    EduClassPO selectClassById(Long classId);

    EduClassPO selectClassByCode(String classCode);

    List<EduClassPO> selectClassesByTeacherId(Long teacherId);

    List<EduClassPO> selectClassesByCondition(Long teacherId, String className, String grade, Integer classStatus);

    int insertClass(EduClassPO eduClassPO);

    int updateClass(Long classId, String className, String grade, String school, Integer joinType, Long updateBy);

    int updateClassStatus(Long classId, Integer status, Long updateBy);

    int deleteClass(Long classId, Long updateBy);

    int updateClassCode(Long classId, String classCode, Long updateBy);

    int updateStudentCount(Long classId, Integer studentCount, Long updateBy);
}
