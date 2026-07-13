package com.edu.service;

import com.edu.pojo.dto.student.StudentJoinClassRequest;
import com.edu.pojo.dto.student.StudentJoinedClassDTO;

import java.util.List;

public interface StudentClassService {
    StudentJoinedClassDTO joinClass(StudentJoinClassRequest request);

    void leaveClass(Long classId);

    List<StudentJoinedClassDTO> listJoinedClasses();
}
