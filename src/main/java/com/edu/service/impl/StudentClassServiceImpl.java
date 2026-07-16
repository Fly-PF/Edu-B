package com.edu.service.impl;

import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.exception.UserErrorException;
import com.edu.pojo.dto.StudentClassCourseDTO;
import com.edu.pojo.dto.StudentClassDetailDTO;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.EduChapterPO;
import com.edu.pojo.po.EduClassPO;
import com.edu.pojo.po.EduClassStudentPO;
import com.edu.pojo.po.EduCourseClassPO;
import com.edu.pojo.po.EduCoursePO;
import com.edu.pojo.po.EduStudyRecordPO;
import com.edu.pojo.po.SysUserPO;
import com.edu.repository.EduChapterRepository;
import com.edu.repository.EduClassRepository;
import com.edu.repository.EduClassStudentRepository;
import com.edu.repository.EduCourseClassRepository;
import com.edu.repository.EduCourseRepository;
import com.edu.repository.EduStudyRecordRepository;
import com.edu.repository.SysUserRepository;
import com.edu.service.StudentClassService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentClassServiceImpl implements StudentClassService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EduClassRepository eduClassRepository;
    private final EduClassStudentRepository eduClassStudentRepository;
    private final EduCourseClassRepository eduCourseClassRepository;
    private final EduCourseRepository eduCourseRepository;
    private final EduChapterRepository eduChapterRepository;
    private final EduStudyRecordRepository eduStudyRecordRepository;
    private final SysUserRepository sysUserRepository;

    private Long getCurrentStudentId() {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new UserErrorException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return loginUser.getUserId();
    }

    private void requireStudentInClass(Long classId) {
        Long studentId = getCurrentStudentId();
        EduClassStudentPO relation = eduClassStudentRepository.selectClassStudent(classId, studentId);
        if (relation == null) {
            throw new UserErrorException(HttpStatus.FORBIDDEN, "未加入该班级");
        }
    }

    private EduClassPO requireActiveClass(Long classId) {
        if (classId == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "班级ID不能为空");
        }
        EduClassPO eduClassPO = eduClassRepository.selectClassById(classId);
        if (eduClassPO == null || Objects.equals(eduClassPO.getDeleted(), 1)) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "班级不存在");
        }
        return eduClassPO;
    }

    @Override
    public StudentClassDetailDTO getStudentClassDetail(Long classId) {
        EduClassPO eduClassPO = requireActiveClass(classId);
        requireStudentInClass(classId);

        Long studentId = getCurrentStudentId();

        SysUserPO teacher = sysUserRepository.selectUserById(eduClassPO.getTeacherId());

        List<StudentClassDetailDTO.AssignedCourseItem> assignedCourses =
                eduCourseClassRepository.selectByClassId(classId).stream()
                        .map(assignment -> buildAssignedCourseItem(assignment, studentId))
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(StudentClassDetailDTO.AssignedCourseItem::getPublishTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();

        return StudentClassDetailDTO.builder()
                .id(eduClassPO.getId())
                .className(eduClassPO.getClassName())
                .school(eduClassPO.getSchool())
                .grade(eduClassPO.getGrade())
                .teacher(StudentClassDetailDTO.TeacherBrief.builder()
                        .teacherId(eduClassPO.getTeacherId())
                        .teacherName(teacher == null ? null : teacher.getRealName())
                        .build())
                .studentCount(eduClassPO.getStudentCount())
                .classStatus(eduClassPO.getStatus())
                .assignedCourses(assignedCourses)
                .build();
    }

    @Override
    public PageResult<StudentClassCourseDTO> listStudentClassCourses(
            Long classId, Integer pageNum, Integer pageSize, String keyword, Integer studyStatus
    ) {
        requireActiveClass(classId);
        requireStudentInClass(classId);

        Long studentId = getCurrentStudentId();
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);

        List<StudentClassCourseDTO> matched = eduCourseClassRepository.selectByClassId(classId).stream()
                .sorted(Comparator.comparing(EduCourseClassPO::getPublishTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(assignment -> buildStudentClassCourseDTO(assignment, studentId))
                .filter(Objects::nonNull)
                .filter(dto -> matchKeyword(dto, keyword))
                .filter(dto -> studyStatus == null || Objects.equals(dto.getStudyStatus(), studyStatus))
                .toList();

        List<StudentClassCourseDTO> records = matched.stream()
                .skip(pageQuery.offset())
                .limit(pageQuery.getPageSize())
                .toList();
        return PageResult.of(matched.size(), pageQuery, records);
    }

    private StudentClassDetailDTO.AssignedCourseItem buildAssignedCourseItem(
            EduCourseClassPO assignment, Long studentId
    ) {
        EduCoursePO course = eduCourseRepository.selectCourseById(assignment.getCourseId());
        if (course == null || Objects.equals(course.getDeleted(), 1)) {
            return null;
        }

        int[] studyInfo = calculateStudyStatusAndProgress(course.getId(), studentId, assignment.getDeadline());

        return StudentClassDetailDTO.AssignedCourseItem.builder()
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .cover(course.getCover())
                .publishTime(formatDateTime(assignment.getPublishTime()))
                .deadline(formatDateTime(assignment.getDeadline()))
                .studyStatus(studyInfo[0])
                .progress(studyInfo[1])
                .build();
    }

    private StudentClassCourseDTO buildStudentClassCourseDTO(EduCourseClassPO assignment, Long studentId) {
        EduCoursePO course = eduCourseRepository.selectCourseById(assignment.getCourseId());
        if (course == null || Objects.equals(course.getDeleted(), 1)) {
            return null;
        }

        int[] studyInfo = calculateStudyStatusAndProgress(course.getId(), studentId, assignment.getDeadline());

        return StudentClassCourseDTO.builder()
                .assignmentId(assignment.getId())
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .cover(course.getCover())
                .grade(course.getGrade())
                .difficulty(course.getDifficulty())
                .courseType(course.getCourseType())
                .totalDuration(course.getTotalDuration())
                .totalChapter(course.getTotalChapter())
                .publishTime(formatDateTime(assignment.getPublishTime()))
                .deadline(formatDateTime(assignment.getDeadline()))
                .studyStatus(studyInfo[0])
                .progress(studyInfo[1])
                .build();
    }

    private int[] calculateStudyStatusAndProgress(Long courseId, Long studentId, LocalDateTime deadline) {
        List<EduChapterPO> chapters = eduChapterRepository.selectChaptersByCourseId(courseId).stream()
                .filter(ch -> !Objects.equals(ch.getDeleted(), 1))
                .sorted(Comparator.comparing(EduChapterPO::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EduChapterPO::getId))
                .toList();

        List<EduStudyRecordPO> records = eduStudyRecordRepository.selectRecordsByStudentId(studentId).stream()
                .filter(r -> Objects.equals(r.getCourseId(), courseId))
                .toList();

        Map<Long, EduStudyRecordPO> recordsByChapter = records.stream()
                .collect(Collectors.toMap(
                        EduStudyRecordPO::getChapterId,
                        r -> r,
                        (a, b) -> {
                            if (a.getLastStudyTime() == null) return b;
                            if (b.getLastStudyTime() == null) return a;
                            return a.getLastStudyTime().isAfter(b.getLastStudyTime()) ? a : b;
                        }
                ));

        if (chapters.isEmpty()) {
            return new int[]{0, 0};
        }

        long finishedCount = recordsByChapter.values().stream()
                .filter(r -> Objects.equals(r.getFinishStatus(), 1))
                .count();
        boolean hasStarted = recordsByChapter.values().stream()
                .anyMatch(r -> normalizeProgress(r.getProgress()) > 0);

        int progressSum = chapters.stream()
                .map(EduChapterPO::getId)
                .map(id -> recordsByChapter.get(id))
                .filter(Objects::nonNull)
                .mapToInt(r -> normalizeProgress(r.getProgress()))
                .sum();
        int progress = Math.round((float) progressSum / chapters.size());

        int studyStatus;
        if (finishedCount >= chapters.size()) {
            studyStatus = 2;
        } else if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
            studyStatus = 3;
        } else if (hasStarted) {
            studyStatus = 1;
        } else {
            studyStatus = 0;
        }

        return new int[]{studyStatus, progress};
    }

    private static int normalizeProgress(Integer progress) {
        return progress == null ? 0 : Math.max(0, Math.min(100, progress));
    }

    private static boolean matchKeyword(StudentClassCourseDTO dto, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return dto.getCourseName() != null && dto.getCourseName().contains(keyword.trim());
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }
}
