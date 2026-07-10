package com.edu.service;

import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.pojo.dto.CourseAssignmentDTO;
import com.edu.pojo.dto.CourseAssignmentReq;
import com.edu.pojo.dto.CourseDeadlineDTO;
import com.edu.pojo.dto.TeacherClassCodeDTO;
import com.edu.pojo.dto.TeacherClassCourseDTO;
import com.edu.pojo.dto.TeacherClassDetailDTO;
import com.edu.pojo.dto.TeacherClassInviteCodeDTO;
import com.edu.pojo.dto.TeacherClassStudentDTO;
import com.edu.pojo.dto.TeacherCourseStudyRecordDTO;
import com.edu.pojo.dto.TeacherStudentCourseStudyRecordDTO;
import com.edu.pojo.po.EduClassPO;
import com.edu.pojo.po.EduCourseClassPO;
import com.edu.pojo.po.EduCoursePO;

public interface TeacherClassService {
    Long getCurrentTeacherId();

    PageQuery normalizePage(Integer pageNum, Integer pageSize);

    EduClassPO requireTeacherClass(Long classId);

    EduCoursePO requireTeacherCourse(Long courseId);

    EduCourseClassPO requireCourseAssignment(Long classId, Long courseId);

    EduCourseClassPO requireTeacherCourseAssignment(Long classId, Long courseId);

    TeacherClassDetailDTO getTeacherClassDetail(Long classId);

    TeacherClassInviteCodeDTO getInviteCode(Long classId);

    TeacherClassCodeDTO refreshInviteCode(Long classId);

    TeacherClassCodeDTO updateInviteCode(Long classId, String classCode);

    PageResult<TeacherClassStudentDTO> listClassStudents(Long classId, Integer pageNum, Integer pageSize, String keyword);

    void removeClassStudent(Long classId, Long studentId);

    PageResult<TeacherClassCourseDTO> listAssignedCourses(Long classId, Integer pageNum, Integer pageSize, String keyword);

    CourseAssignmentDTO assignCourse(CourseAssignmentReq req);

    CourseDeadlineDTO updateCourseDeadline(Long classId, Long courseId, String deadline);

    void removeAssignedCourse(Long classId, Long courseId);

    PageResult<TeacherCourseStudyRecordDTO> listCourseStudyRecords(
            Long classId,
            Long courseId,
            Integer pageNum,
            Integer pageSize,
            String keyword,
            Integer studyStatus
    );

    TeacherStudentCourseStudyRecordDTO getStudentCourseStudyRecords(Long classId, Long courseId, Long studentId);
}
