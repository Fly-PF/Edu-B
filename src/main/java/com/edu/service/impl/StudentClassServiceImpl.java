package com.edu.service.impl;

import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.student.StudentJoinClassRequest;
import com.edu.pojo.dto.student.StudentJoinedClassDTO;
import com.edu.pojo.po.EduClassPO;
import com.edu.pojo.po.EduClassStudentPO;
import com.edu.pojo.po.SysUserPO;
import com.edu.repository.EduClassRepository;
import com.edu.repository.EduClassStudentRepository;
import com.edu.repository.EduCourseClassRepository;
import com.edu.repository.SysUserRepository;
import com.edu.service.StudentClassService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentClassServiceImpl implements StudentClassService {
    private static final String ROLE_STUDENT = "STUDENT";
    private static final int CLASS_STATUS_ACTIVE = 1;
    private static final int JOIN_TYPE_PUBLIC = 2;

    private final EduClassRepository classRepository;
    private final EduClassStudentRepository classStudentRepository;
    private final EduCourseClassRepository courseClassRepository;
    private final SysUserRepository userRepository;

    @Override
    @Transactional
    public StudentJoinedClassDTO joinClass(StudentJoinClassRequest request) {
        UserInfoDTO user = requireStudent();
        EduClassPO joinedClass = resolveJoinTarget(request);
        validateJoinable(joinedClass, request);

        if (classStudentRepository.selectClassStudent(joinedClass.getId(), user.getUserId()) != null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "你已加入该班级");
        }

        LocalDateTime joinTime = LocalDateTime.now();
        classStudentRepository.insertClassStudent(EduClassStudentPO.builder()
                .classId(joinedClass.getId())
                .studentId(user.getUserId())
                .joinTime(joinTime)
                .build());
        refreshStudentCount(joinedClass.getId(), user.getUserId());
        return buildJoinedClassDTO(joinedClass, joinTime);
    }

    @Override
    @Transactional
    public void leaveClass(Long classId) {
        UserInfoDTO user = requireStudent();
        if (classId == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "班级ID不能为空");
        }

        EduClassStudentPO relation = classStudentRepository.selectClassStudent(classId, user.getUserId());
        if (relation == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "未找到你的班级加入记录");
        }

        classStudentRepository.deleteClassStudent(classId, user.getUserId());
        refreshStudentCount(classId, user.getUserId());
    }

    @Override
    public List<StudentJoinedClassDTO> listJoinedClasses() {
        UserInfoDTO user = requireStudent();
        return classStudentRepository.selectClassesByStudentId(user.getUserId()).stream()
                .sorted(Comparator.comparing(EduClassStudentPO::getJoinTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .map(relation -> {
                    EduClassPO joinedClass = classRepository.selectClassById(relation.getClassId());
                    if (joinedClass == null || Objects.equals(joinedClass.getDeleted(), 1)) {
                        return null;
                    }
                    return buildJoinedClassDTO(joinedClass, relation.getJoinTime());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private EduClassPO resolveJoinTarget(StudentJoinClassRequest request) {
        if (request == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请提供班级邀请码或班级ID");
        }
        if (request.getClassId() != null) {
            EduClassPO joinedClass = classRepository.selectClassById(request.getClassId());
            if (joinedClass == null || Objects.equals(joinedClass.getDeleted(), 1)) {
                throw new BaseException(HttpStatus.NOT_FOUND, "班级不存在");
            }
            return joinedClass;
        }
        if (StringUtils.hasText(request.getClassCode())) {
            EduClassPO joinedClass = classRepository.selectClassByCode(request.getClassCode().trim());
            if (joinedClass == null || Objects.equals(joinedClass.getDeleted(), 1)) {
                throw new BaseException(HttpStatus.NOT_FOUND, "邀请码无效或班级不存在");
            }
            return joinedClass;
        }
        throw new BaseException(HttpStatus.BAD_REQUEST, "请提供班级邀请码或班级ID");
    }

    private void validateJoinable(EduClassPO joinedClass, StudentJoinClassRequest request) {
        if (!Objects.equals(joinedClass.getStatus(), CLASS_STATUS_ACTIVE)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "当前班级不可加入");
        }
        if (request.getClassId() != null && !Objects.equals(joinedClass.getJoinType(), JOIN_TYPE_PUBLIC)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "该班级不支持公开加入，请使用邀请码");
        }
    }

    private void refreshStudentCount(Long classId, Long operatorId) {
        Long count = classStudentRepository.countStudentsByClassId(classId);
        classRepository.updateStudentCount(classId, count == null ? 0 : count.intValue(), operatorId);
    }

    private StudentJoinedClassDTO buildJoinedClassDTO(EduClassPO joinedClass, LocalDateTime joinTime) {
        SysUserPO teacher = joinedClass.getTeacherId() == null ? null : userRepository.selectUserById(joinedClass.getTeacherId());
        Long studentCount = classStudentRepository.countStudentsByClassId(joinedClass.getId());
        int assignedCourseCount = courseClassRepository.selectByClassId(joinedClass.getId()).size();
        return StudentJoinedClassDTO.builder()
                .classId(joinedClass.getId())
                .className(joinedClass.getClassName())
                .teacherId(joinedClass.getTeacherId())
                .teacherName(teacher == null ? "" : teacher.getRealName())
                .grade(joinedClass.getGrade())
                .school(joinedClass.getSchool())
                .classCode(joinedClass.getClassCode())
                .joinType(joinedClass.getJoinType())
                .studentCount(studentCount == null ? 0 : studentCount.intValue())
                .assignedCourseCount(assignedCourseCount)
                .status(joinedClass.getStatus())
                .joinTime(joinTime)
                .build();
    }

    private UserInfoDTO requireStudent() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (!ROLE_STUDENT.equals(user.getRoleCode())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "仅学生可操作班级加入");
        }
        return user;
    }
}
