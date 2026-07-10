package com.edu.repository;

import com.edu.pojo.po.EduCourseClassPO;

import java.time.LocalDateTime;
import java.util.List;

public interface EduCourseClassRepository {
    EduCourseClassPO selectCourseClass(Long courseId, Long classId);

    List<EduCourseClassPO> selectByCourseId(Long courseId);

    List<EduCourseClassPO> selectByClassId(Long classId);

    int insertCourseClass(EduCourseClassPO eduCourseClassPO);

    int updateDeadline(Long courseId, Long classId, LocalDateTime deadline);

    int deleteCourseClass(Long courseId, Long classId);
}
