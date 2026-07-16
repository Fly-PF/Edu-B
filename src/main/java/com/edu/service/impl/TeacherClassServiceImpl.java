package com.edu.service.impl;

import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.exception.UserErrorException;
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
import com.edu.pojo.dto.CreateClassReq;
import com.edu.pojo.dto.UpdateClassReq;
import com.edu.pojo.dto.UpdateClassStatusReq;
import com.edu.pojo.dto.TeacherClassListDTO;
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
import com.edu.service.TeacherClassService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherClassServiceImpl implements TeacherClassService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CLASS_CODE_PREFIX = "AI";
    private static final String CLASS_CODE_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int GENERATED_CODE_LENGTH = 6;
    private static final int MAX_CLASS_CODE_LENGTH = 30;
    private static final int MAX_GENERATE_RETRY = 20;

    private final EduClassRepository eduClassRepository;
    private final EduClassStudentRepository eduClassStudentRepository;
    private final EduCourseRepository eduCourseRepository;
    private final EduCourseClassRepository eduCourseClassRepository;
    private final EduChapterRepository eduChapterRepository;
    private final EduStudyRecordRepository eduStudyRecordRepository;
    private final SysUserRepository sysUserRepository;

    @Override
    public Long getCurrentTeacherId() {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new UserErrorException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return loginUser.getUserId();
    }

    @Override
    public PageQuery normalizePage(Integer pageNum, Integer pageSize) {
        return PageQuery.of(pageNum, pageSize);
    }

    @Override
    public EduClassPO requireTeacherClass(Long classId) {
        if (classId == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "班级ID不能为空");
        }

        EduClassPO eduClassPO = eduClassRepository.selectClassById(classId);
        if (eduClassPO == null || Objects.equals(eduClassPO.getDeleted(), 1)) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "班级不存在");
        }

        Long teacherId = getCurrentTeacherId();
        if (!Objects.equals(eduClassPO.getTeacherId(), teacherId)) {
            throw new UserErrorException(HttpStatus.FORBIDDEN, "无权访问该班级");
        }

        return eduClassPO;
    }

    @Override
    public EduCoursePO requireTeacherCourse(Long courseId) {
        if (courseId == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "课程ID不能为空");
        }

        EduCoursePO eduCoursePO = eduCourseRepository.selectCourseById(courseId);
        if (eduCoursePO == null || Objects.equals(eduCoursePO.getDeleted(), 1)) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "课程不存在");
        }

        Long teacherId = getCurrentTeacherId();
        if (!Objects.equals(eduCoursePO.getTeacherId(), teacherId)) {
            throw new UserErrorException(HttpStatus.FORBIDDEN, "无权访问该课程");
        }

        return eduCoursePO;
    }

    @Override
    public EduCourseClassPO requireCourseAssignment(Long classId, Long courseId) {
        if (classId == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "班级ID不能为空");
        }
        if (courseId == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "课程ID不能为空");
        }

        EduCourseClassPO courseClassPO = eduCourseClassRepository.selectCourseClass(courseId, classId);
        if (courseClassPO == null) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "课程未下发到该班级");
        }

        return courseClassPO;
    }

    @Override
    public EduCourseClassPO requireTeacherCourseAssignment(Long classId, Long courseId) {
        requireTeacherClass(classId);
        requireTeacherCourse(courseId);
        return requireCourseAssignment(classId, courseId);
    }

    @Override
    public TeacherClassListDTO createClass(CreateClassReq req) {
        if (req == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "参数不能为空");
        }
        if (!org.springframework.util.StringUtils.hasText(req.getClassName())) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "班级名称不能为空");
        }
        if (!org.springframework.util.StringUtils.hasText(req.getGrade())) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "学段不能为空");
        }
        if (!org.springframework.util.StringUtils.hasText(req.getSchool())) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "所属学校不能为空");
        }
        if (req.getJoinType() == null || (req.getJoinType() != 1 && req.getJoinType() != 2)) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "加入方式无效");
        }

        Long teacherId = getCurrentTeacherId();
        String classCode;
        if (org.springframework.util.StringUtils.hasText(req.getClassCode())) {
            String normalizedCode = normalizeClassCode(req.getClassCode());
            assertClassCodeUnique(normalizedCode, null);
            classCode = normalizedCode;
        } else {
            classCode = generateUniqueClassCode();
        }

        EduClassPO eduClassPO = EduClassPO.builder()
                .className(req.getClassName().trim())
                .teacherId(teacherId)
                .grade(req.getGrade().trim())
                .school(req.getSchool().trim())
                .classCode(classCode)
                .joinType(req.getJoinType())
                .studentCount(0)
                .status(1)
                .createBy(teacherId)
                .updateBy(teacherId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();
        eduClassRepository.insertClass(eduClassPO);

        return TeacherClassListDTO.builder()
                .id(eduClassPO.getId())
                .className(eduClassPO.getClassName())
                .school(eduClassPO.getSchool())
                .grade(eduClassPO.getGrade())
                .classCode(eduClassPO.getClassCode())
                .joinType(eduClassPO.getJoinType())
                .studentCount(0)
                .classStatus(eduClassPO.getStatus())
                .createTime(formatDateTime(eduClassPO.getCreateTime()))
                .build();
    }

    @Override
    public void deleteClass(Long classId) {
        EduClassPO eduClassPO = requireTeacherClass(classId);
        Long teacherId = getCurrentTeacherId();
        eduClassRepository.deleteClass(eduClassPO.getId(), teacherId);
    }

    @Override
    public TeacherClassListDTO updateClass(Long classId, UpdateClassReq req) {
        EduClassPO eduClassPO = requireTeacherClass(classId);

        if (req == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "参数不能为空");
        }
        if (!org.springframework.util.StringUtils.hasText(req.getClassName())) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "班级名称不能为空");
        }
        if (!org.springframework.util.StringUtils.hasText(req.getGrade())) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "学段不能为空");
        }
        if (!org.springframework.util.StringUtils.hasText(req.getSchool())) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "所属学校不能为空");
        }
        if (req.getJoinType() == null || (req.getJoinType() != 1 && req.getJoinType() != 2)) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "加入方式无效");
        }

        Long teacherId = getCurrentTeacherId();
        eduClassRepository.updateClass(
                eduClassPO.getId(),
                req.getClassName().trim(),
                req.getGrade().trim(),
                req.getSchool().trim(),
                req.getJoinType(),
                teacherId
        );

        return TeacherClassListDTO.builder()
                .id(eduClassPO.getId())
                .className(req.getClassName().trim())
                .school(req.getSchool().trim())
                .grade(req.getGrade().trim())
                .joinType(req.getJoinType())
                .classStatus(eduClassPO.getStatus())
                .studentCount(eduClassPO.getStudentCount())
                .createTime(formatDateTime(LocalDateTime.now()))
                .build();
    }

    @Override
    public TeacherClassListDTO updateClassStatus(Long classId, UpdateClassStatusReq req) {
        EduClassPO eduClassPO = requireTeacherClass(classId);

        if (req == null || req.getClassStatus() == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "班级状态不能为空");
        }
        Integer classStatus = req.getClassStatus();
        if (classStatus != 0 && classStatus != 1) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "班级状态值无效");
        }

        Long teacherId = getCurrentTeacherId();
        eduClassRepository.updateClassStatus(eduClassPO.getId(), classStatus, teacherId);

        return TeacherClassListDTO.builder()
                .id(eduClassPO.getId())
                .classStatus(classStatus)
                .createTime(formatDateTime(LocalDateTime.now()))
                .build();
    }

    @Override
    public PageResult<TeacherClassListDTO> listTeacherClasses(
            Integer pageNum,
            Integer pageSize,
            String className,
            String grade,
            Integer classStatus
    ) {
        Long teacherId = getCurrentTeacherId();
        PageQuery pageQuery = normalizePage(pageNum, pageSize);

        List<EduClassPO> allClasses = eduClassRepository.selectClassesByCondition(
                teacherId,
                className,
                grade,
                classStatus
        );

        List<TeacherClassListDTO> records = allClasses.stream()
                .skip(pageQuery.offset())
                .limit(pageQuery.getPageSize())
                .map(this::toTeacherClassListDTO)
                .toList();

        return PageResult.of(allClasses.size(), pageQuery, records);
    }

    private TeacherClassListDTO toTeacherClassListDTO(EduClassPO eduClassPO) {
        return TeacherClassListDTO.builder()
                .id(eduClassPO.getId())
                .className(eduClassPO.getClassName())
                .school(eduClassPO.getSchool())
                .grade(eduClassPO.getGrade())
                .joinType(eduClassPO.getJoinType())
                .studentCount(eduClassPO.getStudentCount())
                .classStatus(eduClassPO.getStatus())
                .createTime(formatDateTime(eduClassPO.getCreateTime()))
                .build();
    }

    @Override
    public TeacherClassDetailDTO getTeacherClassDetail(Long classId) {
        EduClassPO eduClassPO = requireTeacherClass(classId);

        List<TeacherClassDetailDTO.StudentBrief> students = eduClassStudentRepository.selectStudentsByClassId(classId)
                .stream()
                .map(this::toStudentBrief)
                .toList();

        List<TeacherClassDetailDTO.AssignedCourse> assignedCourses = eduCourseClassRepository.selectByClassId(classId)
                .stream()
                .map(this::toAssignedCourse)
                .filter(Objects::nonNull)
                .toList();

        return TeacherClassDetailDTO.builder()
                .id(eduClassPO.getId())
                .className(eduClassPO.getClassName())
                .school(eduClassPO.getSchool())
                .grade(eduClassPO.getGrade())
                .classCode(eduClassPO.getClassCode())
                .joinType(eduClassPO.getJoinType())
                .studentCount(eduClassPO.getStudentCount())
                .classStatus(eduClassPO.getStatus())
                .createTime(formatDateTime(eduClassPO.getCreateTime()))
                .students(students)
                .assignedCourses(assignedCourses)
                .build();
    }

    @Override
    public TeacherClassInviteCodeDTO getInviteCode(Long classId) {
        EduClassPO eduClassPO = requireTeacherClass(classId);
        return TeacherClassInviteCodeDTO.builder()
                .classId(eduClassPO.getId())
                .classCode(eduClassPO.getClassCode())
                .joinType(eduClassPO.getJoinType())
                .build();
    }

    @Override
    public TeacherClassCodeDTO refreshInviteCode(Long classId) {
        EduClassPO eduClassPO = requireTeacherClass(classId);
        String classCode = generateUniqueClassCode();
        eduClassRepository.updateClassCode(eduClassPO.getId(), classCode, getCurrentTeacherId());
        return TeacherClassCodeDTO.builder()
                .classId(eduClassPO.getId())
                .classCode(classCode)
                .build();
    }

    @Override
    public TeacherClassCodeDTO updateInviteCode(Long classId, String classCode) {
        EduClassPO eduClassPO = requireTeacherClass(classId);
        String normalizedClassCode = normalizeClassCode(classCode);
        assertClassCodeUnique(normalizedClassCode, eduClassPO.getId());
        eduClassRepository.updateClassCode(eduClassPO.getId(), normalizedClassCode, getCurrentTeacherId());
        return TeacherClassCodeDTO.builder()
                .classId(eduClassPO.getId())
                .classCode(normalizedClassCode)
                .build();
    }

    @Override
    public PageResult<TeacherClassStudentDTO> listClassStudents(
            Long classId,
            Integer pageNum,
            Integer pageSize,
            String keyword
    ) {
        requireTeacherClass(classId);
        PageQuery pageQuery = normalizePage(pageNum, pageSize);

        List<TeacherClassStudentDTO> matchedStudents = new ArrayList<>();
        for (EduClassStudentPO classStudentPO : eduClassStudentRepository.selectStudentsByClassId(classId)) {
            TeacherClassStudentDTO student = toClassStudentDTO(classStudentPO);
            if (matchStudentKeyword(student, keyword)) {
                matchedStudents.add(student);
            }
        }

        List<TeacherClassStudentDTO> records = matchedStudents.stream()
                .skip(pageQuery.offset())
                .limit(pageQuery.getPageSize())
                .toList();
        return PageResult.of(matchedStudents.size(), pageQuery, records);
    }

    @Override
    public void removeClassStudent(Long classId, Long studentId) {
        EduClassPO eduClassPO = requireTeacherClass(classId);
        if (studentId == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "学生ID不能为空");
        }

        EduClassStudentPO classStudentPO = eduClassStudentRepository.selectClassStudent(classId, studentId);
        if (classStudentPO == null) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "学生不在该班级中");
        }

        eduClassStudentRepository.deleteClassStudent(classId, studentId);
        Long studentCount = eduClassStudentRepository.countStudentsByClassId(classId);
        eduClassRepository.updateStudentCount(eduClassPO.getId(), studentCount.intValue(), getCurrentTeacherId());
    }

    @Override
    public PageResult<TeacherClassCourseDTO> listAssignedCourses(
            Long classId,
            Integer pageNum,
            Integer pageSize,
            String keyword
    ) {
        requireTeacherClass(classId);
        PageQuery pageQuery = normalizePage(pageNum, pageSize);

        List<TeacherClassCourseDTO> matchedCourses = eduCourseClassRepository.selectByClassId(classId)
                .stream()
                .sorted(Comparator.comparing(EduCourseClassPO::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toTeacherClassCourseDTO)
                .filter(Objects::nonNull)
                .filter(course -> matchCourseKeyword(course, keyword))
                .toList();

        List<TeacherClassCourseDTO> records = matchedCourses.stream()
                .skip(pageQuery.offset())
                .limit(pageQuery.getPageSize())
                .toList();
        return PageResult.of(matchedCourses.size(), pageQuery, records);
    }

    @Override
    public CourseAssignmentDTO assignCourse(CourseAssignmentReq req) {
        if (req == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "课程下发参数不能为空");
        }
        EduClassPO eduClassPO = requireTeacherClass(req.getClassId());
        EduCoursePO eduCoursePO = requireTeacherCourse(req.getCourseId());
        if (eduCourseClassRepository.selectCourseClass(req.getCourseId(), req.getClassId()) != null) {
            throw new UserErrorException(HttpStatus.CONFLICT, "课程已下发到该班级");
        }

        EduCourseClassPO courseClassPO = EduCourseClassPO.builder()
                .courseId(req.getCourseId())
                .classId(eduClassPO.getId())
                .publishTime(LocalDateTime.now())
                .deadline(parseDeadline(req.getDeadline()))
                .build();
        eduCourseClassRepository.insertCourseClass(courseClassPO);
        return CourseAssignmentDTO.builder()
                .id(courseClassPO.getId())
                .courseId(eduCoursePO.getId())
                .courseName(eduCoursePO.getCourseName())
                .classId(eduClassPO.getId())
                .className(eduClassPO.getClassName())
                .publishTime(formatDateTime(courseClassPO.getPublishTime()))
                .deadline(formatDateTime(courseClassPO.getDeadline()))
                .build();
    }

    @Override
    public CourseDeadlineDTO updateCourseDeadline(Long classId, Long courseId, String deadline) {
        EduCourseClassPO courseClassPO = requireTeacherCourseAssignment(classId, courseId);
        LocalDateTime parsedDeadline = parseDeadline(deadline);
        eduCourseClassRepository.updateDeadline(courseId, classId, parsedDeadline);
        return CourseDeadlineDTO.builder()
                .classId(courseClassPO.getClassId())
                .courseId(courseClassPO.getCourseId())
                .deadline(formatDateTime(parsedDeadline))
                .build();
    }

    @Override
    public void removeAssignedCourse(Long classId, Long courseId) {
        requireTeacherCourseAssignment(classId, courseId);
        eduCourseClassRepository.deleteCourseClass(courseId, classId);
    }

    @Override
    public PageResult<TeacherCourseStudyRecordDTO> listCourseStudyRecords(
            Long classId,
            Long courseId,
            Integer pageNum,
            Integer pageSize,
            String keyword,
            Integer studyStatus
    ) {
        validateStudyStatus(studyStatus);
        requireTeacherClass(classId);
        EduCoursePO course = requireTeacherCourse(courseId);
        EduCourseClassPO courseClassPO = requireCourseAssignment(classId, courseId);
        PageQuery pageQuery = normalizePage(pageNum, pageSize);

        List<EduChapterPO> chapters = selectActiveChapters(courseId);
        Set<Long> chapterIds = chapters.stream()
                .map(EduChapterPO::getId)
                .collect(Collectors.toSet());
        List<EduClassStudentPO> classStudents = eduClassStudentRepository.selectStudentsByClassId(classId);
        Set<Long> studentIds = classStudents.stream()
                .map(EduClassStudentPO::getStudentId)
                .collect(Collectors.toSet());
        Map<Long, List<EduStudyRecordPO>> recordsByStudent = eduStudyRecordRepository.selectRecordsByCourseId(courseId)
                .stream()
                .filter(record -> studentIds.contains(record.getStudentId()))
                .filter(record -> chapterIds.contains(record.getChapterId()))
                .collect(Collectors.groupingBy(EduStudyRecordPO::getStudentId));

        List<TeacherCourseStudyRecordDTO> matchedRecords = classStudents.stream()
                .map(classStudent -> buildCourseStudyRecordDTO(
                        classStudent,
                        course,
                        chapters,
                        courseClassPO,
                        recordsByStudent.getOrDefault(classStudent.getStudentId(), Collections.emptyList())
                ))
                .filter(record -> matchStudyRecordKeyword(record, keyword))
                .filter(record -> studyStatus == null || Objects.equals(record.getStudyStatus(), studyStatus))
                .toList();

        List<TeacherCourseStudyRecordDTO> records = matchedRecords.stream()
                .skip(pageQuery.offset())
                .limit(pageQuery.getPageSize())
                .toList();
        return PageResult.of(matchedRecords.size(), pageQuery, records);
    }

    @Override
    public TeacherStudentCourseStudyRecordDTO getStudentCourseStudyRecords(Long classId, Long courseId, Long studentId) {
        requireTeacherClass(classId);
        EduCoursePO course = requireTeacherCourse(courseId);
        EduCourseClassPO courseClassPO = requireCourseAssignment(classId, courseId);
        EduClassStudentPO classStudentPO = requireClassStudent(classId, studentId);
        SysUserPO student = sysUserRepository.selectUserById(studentId);

        List<EduChapterPO> chapters = selectActiveChapters(courseId);
        Set<Long> chapterIds = chapters.stream()
                .map(EduChapterPO::getId)
                .collect(Collectors.toSet());
        Map<Long, EduStudyRecordPO> recordsByChapter = eduStudyRecordRepository.selectRecordsByStudentId(studentId)
                .stream()
                .filter(record -> Objects.equals(record.getCourseId(), courseId))
                .filter(record -> chapterIds.contains(record.getChapterId()))
                .collect(Collectors.toMap(
                        EduStudyRecordPO::getChapterId,
                        record -> record,
                        this::newerRecord
                ));

        Integer progress = calculateProgress(chapters, recordsByChapter);
        Integer studyDuration = calculateStudyDuration(recordsByChapter.values().stream().toList());
        Integer finishedChapter = calculateFinishedChapter(recordsByChapter.values().stream().toList());
        Integer studyStatus = calculateStudyStatus(
                chapters.size(),
                finishedChapter,
                hasStudyStarted(recordsByChapter.values().stream().toList()),
                courseClassPO.getDeadline()
        );

        List<TeacherStudentCourseStudyRecordDTO.ChapterStudyRecord> chapterRecords = chapters.stream()
                .map(chapter -> buildChapterStudyRecord(chapter, recordsByChapter.get(chapter.getId())))
                .toList();

        return TeacherStudentCourseStudyRecordDTO.builder()
                .studentId(classStudentPO.getStudentId())
                .studentName(student == null ? null : student.getRealName())
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .progress(progress)
                .studyDuration(studyDuration)
                .studyStatus(studyStatus)
                .chapters(chapterRecords)
                .build();
    }

    private EduClassStudentPO requireClassStudent(Long classId, Long studentId) {
        if (studentId == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "学生ID不能为空");
        }

        EduClassStudentPO classStudentPO = eduClassStudentRepository.selectClassStudent(classId, studentId);
        if (classStudentPO == null) {
            throw new UserErrorException(HttpStatus.NOT_FOUND, "学生不在该班级中");
        }
        return classStudentPO;
    }

    private List<EduChapterPO> selectActiveChapters(Long courseId) {
        return eduChapterRepository.selectChaptersByCourseId(courseId)
                .stream()
                .filter(chapter -> !Objects.equals(chapter.getDeleted(), 1))
                .sorted(Comparator
                        .comparing(EduChapterPO::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EduChapterPO::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private TeacherCourseStudyRecordDTO buildCourseStudyRecordDTO(
            EduClassStudentPO classStudentPO,
            EduCoursePO course,
            List<EduChapterPO> chapters,
            EduCourseClassPO courseClassPO,
            List<EduStudyRecordPO> records
    ) {
        TeacherClassStudentDTO student = toClassStudentDTO(classStudentPO);
        Map<Long, EduStudyRecordPO> recordsByChapter = records.stream()
                .collect(Collectors.toMap(
                        EduStudyRecordPO::getChapterId,
                        record -> record,
                        this::newerRecord
                ));
        List<EduStudyRecordPO> chapterRecords = recordsByChapter.values().stream().toList();
        Integer finishedChapter = calculateFinishedChapter(chapterRecords);
        Integer progress = calculateProgress(chapters, recordsByChapter);
        Integer studyDuration = calculateStudyDuration(chapterRecords);
        Integer studyStatus = calculateStudyStatus(
                chapters.size(),
                finishedChapter,
                hasStudyStarted(chapterRecords),
                courseClassPO.getDeadline()
        );

        return TeacherCourseStudyRecordDTO.builder()
                .studentId(student.getStudentId())
                .studentName(student.getStudentName())
                .studentNo(student.getStudentNo())
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .totalChapter(chapters.size())
                .finishedChapter(finishedChapter)
                .progress(progress)
                .studyDuration(studyDuration)
                .studyStatus(studyStatus)
                .lastStudyTime(formatDateTime(getLastStudyTime(chapterRecords)))
                .build();
    }

    private TeacherStudentCourseStudyRecordDTO.ChapterStudyRecord buildChapterStudyRecord(
            EduChapterPO chapter,
            EduStudyRecordPO record
    ) {
        return TeacherStudentCourseStudyRecordDTO.ChapterStudyRecord.builder()
                .chapterId(chapter.getId())
                .chapterName(chapter.getChapterName())
                .sort(chapter.getSort())
                .duration(chapter.getDuration())
                .resourceId(record == null ? null : record.getResourceId())
                .progress(record == null ? 0 : normalizeProgress(record.getProgress()))
                .studyDuration(record == null ? 0 : defaultNumber(record.getStudyDuration()))
                .finishStatus(record == null ? 0 : defaultNumber(record.getFinishStatus()))
                .lastStudyTime(record == null ? null : formatDateTime(record.getLastStudyTime()))
                .build();
    }

    private Integer calculateFinishedChapter(List<EduStudyRecordPO> records) {
        return (int) records.stream()
                .filter(record -> Objects.equals(record.getFinishStatus(), 1))
                .count();
    }

    private Integer calculateProgress(List<EduChapterPO> chapters, Map<Long, EduStudyRecordPO> recordsByChapter) {
        if (chapters.isEmpty()) {
            return 0;
        }

        int progressTotal = chapters.stream()
                .map(EduChapterPO::getId)
                .map(recordsByChapter::get)
                .filter(Objects::nonNull)
                .mapToInt(record -> normalizeProgress(record.getProgress()))
                .sum();
        return Math.round((float) progressTotal / chapters.size());
    }

    private Integer calculateStudyDuration(List<EduStudyRecordPO> records) {
        return records.stream()
                .map(EduStudyRecordPO::getStudyDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private LocalDateTime getLastStudyTime(List<EduStudyRecordPO> records) {
        return records.stream()
                .map(EduStudyRecordPO::getLastStudyTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private Integer calculateStudyStatus(
            Integer totalChapter,
            Integer finishedChapter,
            boolean started,
            LocalDateTime deadline
    ) {
        if (totalChapter > 0 && Objects.equals(finishedChapter, totalChapter)) {
            return 2;
        }
        if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
            return 3;
        }
        return started ? 1 : 0;
    }

    private boolean hasStudyStarted(List<EduStudyRecordPO> records) {
        return records.stream()
                .anyMatch(record -> normalizeProgress(record.getProgress()) > 0
                        || defaultNumber(record.getStudyDuration()) > 0
                        || record.getLastStudyTime() != null
                        || record.getFinishStatus() != null);
    }

    private EduStudyRecordPO newerRecord(EduStudyRecordPO first, EduStudyRecordPO second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        if (first.getLastStudyTime() == null) {
            return second;
        }
        if (second.getLastStudyTime() == null) {
            return first;
        }
        return first.getLastStudyTime().isAfter(second.getLastStudyTime()) ? first : second;
    }

    private Integer normalizeProgress(Integer progress) {
        if (progress == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, progress));
    }

    private Integer defaultNumber(Integer number) {
        return number == null ? 0 : number;
    }

    private void validateStudyStatus(Integer studyStatus) {
        if (studyStatus != null && (studyStatus < 0 || studyStatus > 3)) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "学习状态只能是0、1、2、3");
        }
    }

    private boolean matchStudyRecordKeyword(TeacherCourseStudyRecordDTO record, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim();
        return contains(record.getStudentName(), normalizedKeyword)
                || contains(record.getStudentNo(), normalizedKeyword);
    }

    private TeacherClassDetailDTO.StudentBrief toStudentBrief(EduClassStudentPO classStudentPO) {
        SysUserPO student = sysUserRepository.selectUserById(classStudentPO.getStudentId());
        String studentName = student == null ? null : student.getRealName();

        return TeacherClassDetailDTO.StudentBrief.builder()
                .studentId(classStudentPO.getStudentId())
                .studentName(studentName)
                .joinTime(formatDateTime(classStudentPO.getJoinTime()))
                .build();
    }

    private TeacherClassStudentDTO toClassStudentDTO(EduClassStudentPO classStudentPO) {
        SysUserPO student = sysUserRepository.selectUserById(classStudentPO.getStudentId());
        return TeacherClassStudentDTO.builder()
                .studentId(classStudentPO.getStudentId())
                .studentName(student == null ? null : student.getRealName())
                .studentNo(student == null ? null : student.getUsername())
                .joinTime(formatDateTime(classStudentPO.getJoinTime()))
                .build();
    }

    private TeacherClassDetailDTO.AssignedCourse toAssignedCourse(EduCourseClassPO courseClassPO) {
        EduCoursePO course = eduCourseRepository.selectCourseById(courseClassPO.getCourseId());
        if (course == null || Objects.equals(course.getDeleted(), 1)) {
            return null;
        }

        return TeacherClassDetailDTO.AssignedCourse.builder()
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .cover(course.getCover())
                .publishTime(formatDateTime(courseClassPO.getPublishTime()))
                .deadline(formatDateTime(courseClassPO.getDeadline()))
                .build();
    }

    private TeacherClassCourseDTO toTeacherClassCourseDTO(EduCourseClassPO courseClassPO) {
        EduCoursePO course = eduCourseRepository.selectCourseById(courseClassPO.getCourseId());
        if (course == null || Objects.equals(course.getDeleted(), 1)) {
            return null;
        }

        return TeacherClassCourseDTO.builder()
                .assignmentId(courseClassPO.getId())
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .cover(course.getCover())
                .grade(course.getGrade())
                .difficulty(course.getDifficulty())
                .courseType(course.getCourseType())
                .totalDuration(course.getTotalDuration())
                .totalChapter(course.getTotalChapter())
                .publishTime(formatDateTime(courseClassPO.getPublishTime()))
                .deadline(formatDateTime(courseClassPO.getDeadline()))
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    private boolean matchStudentKeyword(TeacherClassStudentDTO student, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim();
        return contains(student.getStudentName(), normalizedKeyword)
                || contains(student.getStudentNo(), normalizedKeyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private boolean matchCourseKeyword(TeacherClassCourseDTO course, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return contains(course.getCourseName(), keyword.trim());
    }

    private LocalDateTime parseDeadline(String deadline) {
        if (!StringUtils.hasText(deadline)) {
            return null;
        }
        try {
            return LocalDateTime.parse(deadline.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "截止时间格式应为yyyy-MM-dd HH:mm:ss");
        }
    }

    private String generateUniqueClassCode() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String classCode = generateClassCode();
            if (eduClassRepository.selectClassByCode(classCode) == null) {
                return classCode;
            }
        }
        throw new UserErrorException(HttpStatus.CONFLICT, "班级邀请码生成失败，请重试");
    }

    private String generateClassCode() {
        StringBuilder code = new StringBuilder(CLASS_CODE_PREFIX);
        for (int i = 0; i < GENERATED_CODE_LENGTH; i++) {
            code.append(CLASS_CODE_CHARS.charAt(RANDOM.nextInt(CLASS_CODE_CHARS.length())));
        }
        return code.toString();
    }

    private String normalizeClassCode(String classCode) {
        if (!StringUtils.hasText(classCode)) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "班级邀请码不能为空");
        }
        String normalizedClassCode = classCode.trim();
        if (normalizedClassCode.length() > MAX_CLASS_CODE_LENGTH) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "班级邀请码长度不能超过30个字符");
        }
        return normalizedClassCode;
    }

    private void assertClassCodeUnique(String classCode, Long currentClassId) {
        EduClassPO existsClass = eduClassRepository.selectClassByCode(classCode);
        if (existsClass != null && !Objects.equals(existsClass.getId(), currentClassId)) {
            throw new UserErrorException(HttpStatus.CONFLICT, "班级邀请码已存在");
        }
    }
}
