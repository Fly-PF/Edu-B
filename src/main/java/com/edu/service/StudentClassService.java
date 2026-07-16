package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.dto.StudentClassCourseDTO;
import com.edu.pojo.dto.StudentClassDetailDTO;
import com.edu.pojo.dto.student.StudentJoinClassRequest;
import com.edu.pojo.dto.student.StudentJoinedClassDTO;

import java.util.List;

public interface StudentClassService {
    StudentClassDetailDTO getStudentClassDetail(Long classId);
    StudentJoinedClassDTO joinClass(StudentJoinClassRequest request);

    PageResult<StudentClassCourseDTO> listStudentClassCourses(
            Long classId, Integer pageNum, Integer pageSize, String keyword, Integer studyStatus
    );
    void leaveClass(Long classId);

    List<StudentJoinedClassDTO> listJoinedClasses();
}
