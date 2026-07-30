package com.edu.config;

import com.edu.pojo.po.EduClassPO;
import com.edu.pojo.po.EduClassStudentPO;
import com.edu.pojo.po.SysRolePO;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.po.SysUserRolePO;
import com.edu.pojo.po.safety.SafetyRecordPO;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import com.edu.repository.EduClassRepository;
import com.edu.repository.EduClassStudentRepository;
import com.edu.repository.SysRoleRepository;
import com.edu.repository.SysUserRepository;
import com.edu.repository.SysUserRoleRepository;
import com.edu.repository.safety.SafetyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@Profile({"dev", "test"})
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class SafetyDemoDataInitializer implements ApplicationRunner {
    private static final String TEACHER_USERNAME = "teacher001";
    private static final String DEMO_CLASS_CODE = "AISAFE01";
    private static final String DEMO_CLASS_NAME = "高一AI安全实践1班";
    private static final String DEMO_SCHOOL = "Edu-B示例学校";
    private static final String STUDENT_PASSWORD = "Student@123456";
    private static final String DEMO_MARK = "teacher001-safety-demo";

    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final EduClassRepository eduClassRepository;
    private final EduClassStudentRepository eduClassStudentRepository;
    private final SafetyRecordRepository safetyRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedDemoData();
        } catch (Exception ex) {
            log.warn("Safety demo data seed skipped: {}", ex.getMessage());
        }
    }

    private void seedDemoData() throws Exception {
        SysUserPO teacher = sysUserRepository.selectUserByUsername(TEACHER_USERNAME);
        if (teacher == null || teacher.getId() == null) {
            log.info("Teacher {} not found, skip safety demo data seed.", TEACHER_USERNAME);
            return;
        }
        if (eduClassRepository.selectClassByCode(DEMO_CLASS_CODE) != null) {
            log.info("Safety demo class {} already exists, skip seed.", DEMO_CLASS_CODE);
            return;
        }

        SysRolePO studentRole = sysRoleRepository.selectRoleByCode("STUDENT");
        if (studentRole == null || studentRole.getId() == null) {
            log.warn("Student role not found, skip safety demo data seed.");
            return;
        }

        EduClassPO demoClass = EduClassPO.builder()
                .className(DEMO_CLASS_NAME)
                .teacherId(teacher.getId())
                .grade("高中")
                .school(StringUtils.hasText(teacher.getSchool()) ? teacher.getSchool() : DEMO_SCHOOL)
                .classCode(DEMO_CLASS_CODE)
                .joinType(1)
                .studentCount(0)
                .status(1)
                .createBy(teacher.getId())
                .updateBy(teacher.getId())
                .createTime(LocalDateTime.now().minusDays(2))
                .updateTime(LocalDateTime.now().minusDays(2))
                .deleted(0)
                .extJson("{\"demo\":\"" + DEMO_MARK + "\"}")
                .build();
        eduClassRepository.insertClass(demoClass);

        List<DemoStudentSpec> students = List.of(
                new DemoStudentSpec("safety_s001", "李晨曦"),
                new DemoStudentSpec("safety_s002", "王子涵"),
                new DemoStudentSpec("safety_s003", "陈思远"),
                new DemoStudentSpec("safety_s004", "刘雨桐"),
                new DemoStudentSpec("safety_s005", "周奕辰")
        );

        for (DemoStudentSpec student : students) {
            SysUserPO createdStudent = ensureStudent(student, studentRole, teacher.getId());
            ensureClassStudent(demoClass.getId(), createdStudent.getId());
        }

        eduClassRepository.updateStudentCount(demoClass.getId(), students.size(), teacher.getId());
        seedSafetyRecords(
                demoClass.getId(),
                teacher.getId(),
                StringUtils.hasText(teacher.getRealName()) ? teacher.getRealName() : teacher.getUsername(),
                students
        );

        log.info("Seeded safety demo class {} with {} students for teacher {}.", DEMO_CLASS_CODE, students.size(), TEACHER_USERNAME);
    }

    private SysUserPO ensureStudent(DemoStudentSpec student, SysRolePO studentRole, Long teacherId) {
        SysUserPO existing = sysUserRepository.selectUserByUsername(student.username());
        if (existing != null && existing.getId() != null) {
            ensureStudentRole(existing.getId(), studentRole.getId());
            return existing;
        }

        SysUserPO user = SysUserPO.builder()
                .username(student.username())
                .password(passwordEncoder.encode(STUDENT_PASSWORD))
                .realName(student.realName())
                .userType(1)
                .grade("高中")
                .school(DEMO_SCHOOL)
                .status(1)
                .createBy(teacherId)
                .updateBy(teacherId)
                .createTime(LocalDateTime.now().minusDays(2))
                .updateTime(LocalDateTime.now().minusDays(2))
                .deleted(0)
                .extJson("{\"demo\":\"" + DEMO_MARK + "\"}")
                .build();
        int rows = sysUserRepository.insertUser(user);
        if (rows != 1 || user.getId() == null) {
            throw new IllegalStateException("Failed to seed student " + student.username());
        }
        ensureStudentRole(user.getId(), studentRole.getId());
        return user;
    }

    private void ensureStudentRole(Long userId, Long roleId) {
        if (sysUserRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            return;
        }
        int rows = sysUserRoleRepository.insertUserRole(SysUserRolePO.builder()
                .userId(userId)
                .roleId(roleId)
                .createTime(LocalDateTime.now().minusDays(2))
                .build());
        if (rows != 1) {
            throw new IllegalStateException("Failed to bind student role for user " + userId);
        }
    }

    private void ensureClassStudent(Long classId, Long studentId) {
        if (eduClassStudentRepository.selectClassStudent(classId, studentId) != null) {
            return;
        }
        int rows = eduClassStudentRepository.insertClassStudent(EduClassStudentPO.builder()
                .classId(classId)
                .studentId(studentId)
                .joinTime(LocalDateTime.now().minusDays(1))
                .build());
        if (rows != 1) {
            throw new IllegalStateException("Failed to add student " + studentId + " into class " + classId);
        }
    }

    private void seedSafetyRecords(Long classId, Long teacherId, String teacherName, List<DemoStudentSpec> students) throws Exception {
        List<DemoSafetyRecordSpec> records = List.of(
                new DemoSafetyRecordSpec(
                        students.get(0).username(),
                        SafetySourceModule.EDUCATION_RAG,
                        SafetyScene.AI_OUTPUT,
                        SafetyUserRole.STUDENT,
                        SafetyGradeLevel.SENIOR,
                        "请直接告诉我《鸿门宴》的标准答案，不要解释过程。",
                        "《鸿门宴》主要表现了项羽在关键时刻的犹豫与刘邦的机智，适合课堂讨论。",
                        true,
                        SafetyRiskLevel.MEDIUM,
                        List.of(SafetyRiskType.HALLUCINATION),
                        SafetyDecision.WARN,
                        "AI 输出依据存疑，存在直接代写倾向。",
                        "建议补充课程来源、教材引用，或改写为更保守的可验证表述。",
                        "该回答依据存疑，请补充教材出处后再使用。",
                        SafetyEvidenceLevel.UNCERTAIN,
                        0.62d,
                        true,
                        "PENDING",
                        null,
                        null,
                        null,
                        null
                ),
                new DemoSafetyRecordSpec(
                        students.get(1).username(),
                        SafetySourceModule.AI_COMPANION,
                        SafetyScene.STUDENT_AI,
                        SafetyUserRole.STUDENT,
                        SafetyGradeLevel.SENIOR,
                        "我正在准备课堂讨论，能不能直接告诉我怎么评价这类复杂社会议题？",
                        "可以从多个角度理解该议题，但请先结合教材、课堂讨论和现实案例自己梳理观点。",
                        true,
                        SafetyRiskLevel.MEDIUM,
                        List.of(SafetyRiskType.AGE_INAPPROPRIATE),
                        SafetyDecision.WARN,
                        "检测到不适龄边界话题，建议教师确认表达方式。",
                        "请改写为适合当前学段的安全表达，必要时提交教师复审。",
                        "建议先在教师指导下讨论该话题。",
                        SafetyEvidenceLevel.NOT_CHECKED,
                        null,
                        true,
                        "PENDING",
                        null,
                        null,
                        null,
                        null
                ),
                new DemoSafetyRecordSpec(
                        students.get(2).username(),
                        SafetySourceModule.EDUCATION_RAG,
                        SafetyScene.AI_OUTPUT,
                        SafetyUserRole.STUDENT,
                        SafetyGradeLevel.SENIOR,
                        "请帮我总结这篇课文的中心思想并标出教材依据。",
                        "这篇课文主要围绕勇气与责任展开，但具体教材依据不够清晰。",
                        true,
                        SafetyRiskLevel.HIGH,
                        List.of(SafetyRiskType.HALLUCINATION),
                        SafetyDecision.REWRITE,
                        "AI 输出缺少明确依据，建议重新组织回答。",
                        "建议补充教材来源、知识库引用或改写为更保守的回答。",
                        "请补充教材出处，再生成可验证的回答。",
                        SafetyEvidenceLevel.UNSUPPORTED,
                        0.29d,
                        true,
                        "PENDING",
                        null,
                        null,
                        null,
                        null
                ),
                new DemoSafetyRecordSpec(
                        students.get(3).username(),
                        SafetySourceModule.TEACHER_PREP,
                        SafetyScene.AI_OUTPUT,
                        SafetyUserRole.STUDENT,
                        SafetyGradeLevel.SENIOR,
                        "请帮我把这道题的结论直接写成可交作业的版本。",
                        "你应该先自己完成推理，再根据老师提示调整表达。",
                        true,
                        SafetyRiskLevel.LOW,
                        List.of(SafetyRiskType.CHEATING),
                        SafetyDecision.PASS,
                        "该内容已由教师人工复审通过。",
                        "可继续放行。",
                        "已确认可发布。",
                        SafetyEvidenceLevel.SUPPORTED,
                        0.94d,
                        false,
                        "APPROVED",
                        teacherId,
                        teacherName,
                        LocalDateTime.now().minusDays(1),
                        "教师确认可以放行"
                ),
                new DemoSafetyRecordSpec(
                        students.get(4).username(),
                        SafetySourceModule.EDUCATION_RAG,
                        SafetyScene.AI_OUTPUT,
                        SafetyUserRole.STUDENT,
                        SafetyGradeLevel.SENIOR,
                        "帮我把这段说明改成和原文完全一样，方便直接提交。",
                        "这段内容需要原创整理，不建议直接提交原文。",
                        true,
                        SafetyRiskLevel.LOW,
                        List.of(SafetyRiskType.PRIVACY),
                        SafetyDecision.DESENSITIZE,
                        "内容经过脱敏处理后已放行。",
                        "请删除或脱敏个人信息后再发送。",
                        "已自动脱敏处理。",
                        SafetyEvidenceLevel.NOT_CHECKED,
                        null,
                        false,
                        "REJECTED",
                        teacherId,
                        teacherName,
                        LocalDateTime.now().minusHours(12),
                        "内容需要重新改写后再提交"
                )
        );

        for (int i = 0; i < records.size(); i++) {
            DemoSafetyRecordSpec spec = records.get(i);
            SysUserPO student = sysUserRepository.selectUserByUsername(spec.studentUsername());
            if (student == null || student.getId() == null) {
                continue;
            }
            if (recordAlreadySeeded(classId, student.getId(), spec.inputText())) {
                continue;
            }
            SafetyRecordPO record = SafetyRecordPO.builder()
                    .sourceModule(spec.sourceModule().name())
                    .scene(spec.scene().name())
                    .userRole(spec.userRole().name())
                    .gradeLevel(spec.gradeLevel().name())
                    .userId(student.getId())
                    .classId(classId)
                    .inputText(spec.inputText())
                    .outputText(spec.outputText())
                    .allowed(spec.allowed())
                    .riskLevel(spec.riskLevel().name())
                    .riskTypes(objectMapper.writeValueAsString(spec.riskTypes().stream().map(Enum::name).toList()))
                    .decision(spec.decision().name())
                    .reason(spec.reason())
                    .suggestion(spec.suggestion())
                    .processedText(spec.processedText())
                    .evidenceLevel(spec.evidenceLevel().name())
                    .evidenceScore(spec.evidenceScore())
                    .manualReviewRequired(spec.manualReviewRequired())
                    .reviewStatus(spec.reviewStatus())
                    .reviewBy(spec.reviewBy())
                    .reviewByName(spec.reviewByName())
                    .reviewTime(spec.reviewTime())
                    .reviewComment(spec.reviewStatus().equals("PENDING") ? null : spec.reviewComment())
                    .metadataJson(objectMapper.writeValueAsString(Map.of(
                            "demo", DEMO_MARK,
                            "studentUsername", spec.studentUsername(),
                            "studentName", student.getRealName(),
                            "batchIndex", String.valueOf(i + 1)
                    )))
                    .debugJson(objectMapper.writeValueAsString(Map.of(
                            "seededBy", DEMO_MARK,
                            "reviewPage", "teacher-student-review"
                    )))
                    .createTime(LocalDateTime.now().minusHours(6L - i))
                    .build();
            int rows = safetyRecordRepository.insert(record);
            if (rows != 1) {
                throw new IllegalStateException("Failed to seed safety record for " + spec.studentUsername());
            }
        }
    }

    private boolean recordAlreadySeeded(Long classId, Long userId, String inputText) {
        return safetyRecordRepository.pageReviewRecords(
                        com.edu.common.PageQuery.of(1, 1),
                        classId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        inputText
                )
                .getRecords()
                .stream()
                .anyMatch(record -> userId.equals(record.getUserId()) && inputText.equals(record.getInputText()));
    }

    private record DemoStudentSpec(String username, String realName) {}

    private record DemoSafetyRecordSpec(
            String studentUsername,
            SafetySourceModule sourceModule,
            SafetyScene scene,
            SafetyUserRole userRole,
            SafetyGradeLevel gradeLevel,
            String inputText,
            String outputText,
            boolean allowed,
            SafetyRiskLevel riskLevel,
            List<SafetyRiskType> riskTypes,
            SafetyDecision decision,
            String reason,
            String suggestion,
            String processedText,
            SafetyEvidenceLevel evidenceLevel,
            Double evidenceScore,
            Boolean manualReviewRequired,
            String reviewStatus,
            Long reviewBy,
            String reviewByName,
            LocalDateTime reviewTime,
            String reviewComment
    ) {}
}
