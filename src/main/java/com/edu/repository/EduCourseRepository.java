package com.edu.repository;

import com.edu.pojo.po.EduCoursePO;

import java.util.List;

public interface EduCourseRepository {
    EduCoursePO selectCourseById(Long courseId);

    List<EduCoursePO> selectCoursesByTeacherId(Long teacherId);

    List<EduCoursePO> selectPublicCourses();
}
