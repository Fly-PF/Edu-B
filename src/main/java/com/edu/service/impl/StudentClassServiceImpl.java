package com.edu.service.impl;

import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.exception.BaseException;
import com.edu.exception.UserErrorException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.StudentClassCourseDTO;
import com.edu.pojo.dto.StudentClassDetailDTO;
import com.edu.pojo.dto.student.StudentJoinClassRequest;
import com.edu.pojo.dto.student.StudentJoinedClassDTO;
import com.edu.pojo.po.EduClassPO;
import com.edu.pojo.po.EduClassStudentPO;
import com.edu.pojo.po.EduChapterPO;
import com.edu.pojo.po.EduCourseClassPO;
import com.edu.pojo.po.EduCoursePO;
import com.edu.pojo.po.EduStudyRecordPO;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class StudentClassServiceImpl implements StudentClassService {
    private static final String ROLE_STUDENT = "STUDENT";
    private static final int CLASS_STATUS_ACTIVE = 1;
    private static final int JOIN_TYPE_PUBLIC = 2;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EduClassRepository classRepository;
    private final EduClassStudentRepository classStudentRepository;
    private final EduCourseClassRepository courseClassRepository;
    private final SysUserRepository userRepository;
    private final com.edu.repository.EduCourseRepository courseRepository;
    private final com.edu.repository.EduChapterRepository chapterRepository;
    private final com.edu.repository.EduStudyRecordRepository studyRecordRepository;

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

    @Override
    public StudentClassDetailDTO getStudentClassDetail(Long classId) {
        UserInfoDTO user = requireStudent();
        EduClassPO clazz = classRepository.selectClassById(classId);
        if (clazz == null || Objects.equals(clazz.getDeleted(), 1)) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "班级不存在");
        }
        if (classStudentRepository.selectClassStudent(classId, user.getUserId()) == null) {
            throw new UserErrorException(HttpStatus.FORBIDDEN, "未加入该班级");
        }
        SysUserPO teacher = userRepository.selectUserById(clazz.getTeacherId());
        Long studentId = user.getUserId();
        List<StudentClassDetailDTO.AssignedCourseItem> assignedCourses =
                courseClassRepository.selectByClassId(classId).stream()
                        .map(a -> buildAssignedItem(a, studentId)).filter(Objects::nonNull)
                        .sorted(Comparator.comparing(
                                StudentClassDetailDTO.AssignedCourseItem::getPublishTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();
        return StudentClassDetailDTO.builder()
                .id(clazz.getId()).className(clazz.getClassName())
                .school(clazz.getSchool()).grade(clazz.getGrade())
                .teacher(StudentClassDetailDTO.TeacherBrief.builder()
                        .teacherId(clazz.getTeacherId())
                        .teacherName(teacher == null ? null : teacher.getRealName()).build())
                .studentCount(clazz.getStudentCount()).classStatus(clazz.getStatus())
                .assignedCourses(assignedCourses).build();
    }

    @Override
    public PageResult<StudentClassCourseDTO> listStudentClassCourses(
            Long classId, Integer pageNum, Integer pageSize, String keyword, Integer studyStatus) {
        UserInfoDTO user = requireStudent();
        EduClassPO clazz = classRepository.selectClassById(classId);
        if (clazz == null || Objects.equals(clazz.getDeleted(), 1)) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "班级不存在");
        }
        if (classStudentRepository.selectClassStudent(classId, user.getUserId()) == null) {
            throw new UserErrorException(HttpStatus.FORBIDDEN, "未加入该班级");
        }
        Long studentId = user.getUserId();
        PageQuery pq = PageQuery.of(pageNum, pageSize);
        List<StudentClassCourseDTO> matched = courseClassRepository.selectByClassId(classId).stream()
                .sorted(Comparator.comparing(EduCourseClassPO::getPublishTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(a -> buildCourseDTO(a, studentId)).filter(Objects::nonNull)
                .filter(d -> !StringUtils.hasText(keyword) || (d.getCourseName() != null && d.getCourseName().contains(keyword.trim())))
                .filter(d -> studyStatus == null || Objects.equals(d.getStudyStatus(), studyStatus))
                .toList();
        List<StudentClassCourseDTO> records = matched.stream().skip(pq.offset()).limit(pq.getPageSize()).toList();
        return PageResult.of(matched.size(), pq, records);
    }

    private StudentClassDetailDTO.AssignedCourseItem buildAssignedItem(EduCourseClassPO a, Long sid) {
        EduCoursePO c = courseRepository.selectCourseById(a.getCourseId());
        if (c == null || Objects.equals(c.getDeleted(), 1)) return null;
        int[] info = calcStudyInfo(c.getId(), sid, a.getDeadline());
        return StudentClassDetailDTO.AssignedCourseItem.builder()
                .courseId(c.getId()).courseName(c.getCourseName()).cover(c.getCover())
                .publishTime(fmt(a.getPublishTime())).deadline(fmt(a.getDeadline()))
                .studyStatus(info[0]).progress(info[1]).build();
    }

    private StudentClassCourseDTO buildCourseDTO(EduCourseClassPO a, Long sid) {
        EduCoursePO c = courseRepository.selectCourseById(a.getCourseId());
        if (c == null || Objects.equals(c.getDeleted(), 1)) return null;
        int[] info = calcStudyInfo(c.getId(), sid, a.getDeadline());
        return StudentClassCourseDTO.builder()
                .assignmentId(a.getId()).courseId(c.getId()).courseName(c.getCourseName())
                .cover(c.getCover()).grade(c.getGrade()).difficulty(c.getDifficulty())
                .courseType(c.getCourseType()).totalDuration(c.getTotalDuration())
                .totalChapter(c.getTotalChapter())
                .publishTime(fmt(a.getPublishTime())).deadline(fmt(a.getDeadline()))
                .studyStatus(info[0]).progress(info[1]).build();
    }

    private int[] calcStudyInfo(Long courseId, Long studentId, LocalDateTime deadline) {
        List<EduChapterPO> chapters = chapterRepository.selectChaptersByCourseId(courseId).stream()
                .filter(ch -> !Objects.equals(ch.getDeleted(), 1))
                .sorted(Comparator.comparing(EduChapterPO::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EduChapterPO::getId)).toList();
        if (chapters.isEmpty()) return new int[]{0, 0};
        Map<Long, EduStudyRecordPO> recordsByChapter = studyRecordRepository
                .selectRecordsByStudentId(studentId).stream()
                .filter(r -> Objects.equals(r.getCourseId(), courseId))
                .collect(Collectors.toMap(EduStudyRecordPO::getChapterId, r -> r, (a, b) -> {
                    if (a.getLastStudyTime() == null) return b;
                    if (b.getLastStudyTime() == null) return a;
                    return a.getLastStudyTime().isAfter(b.getLastStudyTime()) ? a : b;
                }));
        long finishedCount = recordsByChapter.values().stream()
                .filter(r -> Objects.equals(r.getFinishStatus(), 1)).count();
        boolean hasStarted = recordsByChapter.values().stream()
                .anyMatch(r -> normProgress(r.getProgress()) > 0);
        int progressSum = chapters.stream().map(EduChapterPO::getId)
                .map(id -> recordsByChapter.get(id)).filter(Objects::nonNull)
                .mapToInt(r -> normProgress(r.getProgress())).sum();
        int status;
        if (finishedCount >= chapters.size()) status = 2;
        else if (deadline != null && LocalDateTime.now().isAfter(deadline)) status = 3;
        else if (hasStarted) status = 1;
        else status = 0;
        return new int[]{status, Math.round((float) progressSum / chapters.size())};
    }

    private static int normProgress(Integer p) { return p == null ? 0 : Math.max(0, Math.min(100, p)); }
    private static String fmt(LocalDateTime dt) { return dt == null ? null : DATE_TIME_FMT.format(dt); }

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
