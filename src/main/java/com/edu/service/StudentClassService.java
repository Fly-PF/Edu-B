package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.dto.StudentClassCourseDTO;
import com.edu.pojo.dto.StudentClassDetailDTO;

public interface StudentClassService {
    StudentClassDetailDTO getStudentClassDetail(Long classId);

    PageResult<StudentClassCourseDTO> listStudentClassCourses(
            Long classId, Integer pageNum, Integer pageSize, String keyword, Integer studyStatus
    );
}
