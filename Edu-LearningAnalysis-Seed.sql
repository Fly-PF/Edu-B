-- 学情分析模块补数脚本
-- 仅创建本模块的行动卡表，并为当前已有 teacher / student 演示账号补齐课程、进度和干预数据。
-- 可重复执行；不会 DROP 表、不会删除或修改其他模块的数据。

USE edu;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS edu_learning_intervention
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id         BIGINT        NOT NULL COMMENT '班级ID',
    course_id        BIGINT        NOT NULL COMMENT '课程ID',
    student_id       BIGINT        NOT NULL COMMENT '学生ID',
    teacher_id       BIGINT        NOT NULL COMMENT '下发教师ID',
    risk_score       INT           NOT NULL DEFAULT 0 COMMENT '下发时的风险分快照',
    title            VARCHAR(200)  NOT NULL COMMENT '行动卡标题',
    task_description VARCHAR(1000) NOT NULL COMMENT '学生执行任务',
    status           VARCHAR(30)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/READY_FOR_REVIEW/CLOSED',
    student_feedback VARCHAR(1000) COMMENT '学生学习反馈',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at        DATETIME COMMENT '教师闭环时间',
    INDEX idx_learning_intervention_class (class_id),
    INDEX idx_learning_intervention_student (student_id),
    INDEX idx_learning_intervention_status (status)
) COMMENT '学情分析行动卡';

-- 该项目当前已有的演示账号：teacher（教师）、student（学生），及其班级。
SET @learning_teacher_id := (
    SELECT id FROM sys_user WHERE username = 'teacher' AND user_type = 2 AND deleted = 0 LIMIT 1
);
SET @learning_student_id := (
    SELECT id FROM sys_user WHERE username = 'student' AND user_type = 1 AND deleted = 0 LIMIT 1
);
SET @learning_class_id := (
    SELECT id FROM edu_class
    WHERE teacher_id = @learning_teacher_id AND deleted = 0
    ORDER BY id
    LIMIT 1
);

-- 独立的学情分析体验账号，密码均为 123456；仅用于本地验收，不修改已有账号密码。
INSERT INTO sys_user
    (id, username, password, real_name, user_type, grade, school, status, create_by, update_by)
SELECT 92001, 'learning_teacher', '$2a$10$fPLyrSvzxqgYAZi7t48j5u/BUvXScaTdytk1nbE80FhRyUQPcE4Hi',
       '学情演示教师', 2, '高中', '星河实验中学', 1, 92001, 92001
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'learning_teacher');
INSERT INTO sys_user
    (id, username, password, real_name, user_type, grade, school, status, create_by, update_by)
SELECT 92002, 'learning_student', '$2a$10$fPLyrSvzxqgYAZi7t48j5u/BUvXScaTdytk1nbE80FhRyUQPcE4Hi',
       '学情演示学生', 1, '高中', '星河实验中学', 1, 92001, 92001
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'learning_student');

SET @learning_demo_teacher_id := (SELECT id FROM sys_user WHERE username = 'learning_teacher' AND deleted = 0 LIMIT 1);
SET @learning_demo_student_id := (SELECT id FROM sys_user WHERE username = 'learning_student' AND deleted = 0 LIMIT 1);
SET @student_role_id := (SELECT id FROM sys_role WHERE role_code = 'STUDENT' AND deleted = 0 LIMIT 1);
SET @teacher_role_id := (SELECT id FROM sys_role WHERE role_code = 'TEACHER' AND deleted = 0 LIMIT 1);

INSERT INTO sys_user_role (user_id, role_id)
SELECT @learning_demo_teacher_id, @teacher_role_id
WHERE @learning_demo_teacher_id IS NOT NULL
  AND @teacher_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role WHERE user_id = @learning_demo_teacher_id AND role_id = @teacher_role_id
  );
INSERT INTO sys_user_role (user_id, role_id)
SELECT @learning_demo_student_id, @student_role_id
WHERE @learning_demo_student_id IS NOT NULL
  AND @student_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role WHERE user_id = @learning_demo_student_id AND role_id = @student_role_id
  );

INSERT INTO edu_class
    (id, class_name, teacher_id, grade, school, class_code, join_type, student_count, status, create_by, update_by)
SELECT 92001, '学情分析演示班', @learning_demo_teacher_id, '高中', '星河实验中学', 'LEARN2026', 1, 1, 1,
       @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE @learning_demo_teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_class WHERE class_code = 'LEARN2026');

SET @learning_demo_class_id := (SELECT id FROM edu_class WHERE class_code = 'LEARN2026' AND deleted = 0 LIMIT 1);

INSERT INTO edu_class_student (class_id, student_id, join_time)
SELECT @learning_demo_class_id, @learning_demo_student_id, DATE_SUB(NOW(), INTERVAL 20 DAY)
WHERE @learning_demo_class_id IS NOT NULL
  AND @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM edu_class_student
      WHERE class_id = @learning_demo_class_id AND student_id = @learning_demo_student_id
  );

-- 两门关联课程：一门存在明确风险，一门学习稳定，用于展示风险分层和对比。
INSERT INTO edu_course
    (id, course_name, grade, difficulty, course_type, teacher_id, intro, total_duration, total_chapter, is_public, status, create_by, update_by)
SELECT 91001, 'Python 数据分析基础', '高中', 1, 1, @learning_teacher_id,
       '围绕数据读取、清洗、可视化和结论表达，完成一个可复现的数据分析任务。',
       160, 4, 0, 1, @learning_teacher_id, @learning_teacher_id
WHERE @learning_teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_course WHERE id = 91001);

INSERT INTO edu_course
    (id, course_name, grade, difficulty, course_type, teacher_id, intro, total_duration, total_chapter, is_public, status, create_by, update_by)
SELECT 91002, '机器学习入门', '高中', 2, 1, @learning_teacher_id,
       '理解监督学习、训练集与验证集、分类特征和模型评估的完整流程。',
       180, 4, 0, 1, @learning_teacher_id, @learning_teacher_id
WHERE @learning_teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_course WHERE id = 91002);

INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91101, 91001, '数据表与变量类型', 1, 35, @learning_teacher_id, @learning_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91001)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91101);
INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91102, 91001, '缺失值与数据清洗', 2, 40, @learning_teacher_id, @learning_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91001)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91102);
INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91103, 91001, '分组统计与可视化', 3, 45, @learning_teacher_id, @learning_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91001)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91103);
INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91104, 91001, '数据分析小项目', 4, 40, @learning_teacher_id, @learning_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91001)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91104);

INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91201, 91002, '监督学习的基本问题', 1, 40, @learning_teacher_id, @learning_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91002)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91201);
INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91202, 91002, '训练集与验证集', 2, 45, @learning_teacher_id, @learning_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91002)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91202);
INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91203, 91002, '分类模型与特征', 3, 45, @learning_teacher_id, @learning_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91002)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91203);
INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91204, 91002, '模型评估与复盘', 4, 50, @learning_teacher_id, @learning_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91002)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91204);

INSERT INTO edu_course_class (course_id, class_id, publish_time, deadline)
SELECT 91001, @learning_class_id, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY)
WHERE @learning_class_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM edu_course_class WHERE course_id = 91001 AND class_id = @learning_class_id
  );
INSERT INTO edu_course_class (course_id, class_id, publish_time, deadline)
SELECT 91002, @learning_class_id, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_ADD(NOW(), INTERVAL 16 DAY)
WHERE @learning_class_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM edu_course_class WHERE course_id = 91002 AND class_id = @learning_class_id
  );

INSERT INTO edu_course_class (course_id, class_id, publish_time, deadline)
SELECT 91001, @learning_demo_class_id, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY)
WHERE @learning_demo_class_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM edu_course_class WHERE course_id = 91001 AND class_id = @learning_demo_class_id
  );
INSERT INTO edu_course_class (course_id, class_id, publish_time, deadline)
SELECT 91002, @learning_demo_class_id, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_ADD(NOW(), INTERVAL 16 DAY)
WHERE @learning_demo_class_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM edu_course_class WHERE course_id = 91002 AND class_id = @learning_demo_class_id
  );

-- 学生真实持久化的学习过程：数据分析课久未学习且临近截止，机器学习课保持稳定学习。
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_student_id, 91001, 91101, 100, 36, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)
WHERE @learning_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_student_id AND chapter_id = 91101);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_student_id, 91001, 91102, 45, 22, 0, DATE_SUB(NOW(), INTERVAL 10 DAY)
WHERE @learning_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_student_id AND chapter_id = 91102);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_student_id, 91002, 91201, 100, 42, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @learning_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_student_id AND chapter_id = 91201);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_student_id, 91002, 91202, 100, 46, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @learning_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_student_id AND chapter_id = 91202);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_student_id, 91002, 91203, 70, 32, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @learning_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_student_id AND chapter_id = 91203);

INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_demo_student_id, 91001, 91101, 100, 36, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_demo_student_id AND chapter_id = 91101);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_demo_student_id, 91001, 91102, 45, 22, 0, DATE_SUB(NOW(), INTERVAL 10 DAY)
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_demo_student_id AND chapter_id = 91102);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_demo_student_id, 91002, 91201, 100, 42, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_demo_student_id AND chapter_id = 91201);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_demo_student_id, 91002, 91202, 100, 46, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_demo_student_id AND chapter_id = 91202);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_demo_student_id, 91002, 91203, 70, 32, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_demo_student_id AND chapter_id = 91203);

-- 行动卡由教师下发，学生可在页面提交反馈并进入教师复核闭环。
INSERT INTO edu_learning_intervention
    (class_id, course_id, student_id, teacher_id, risk_score, title, task_description, status, created_at, updated_at)
SELECT @learning_class_id, 91001, @learning_student_id, @learning_teacher_id, 82,
       '补齐数据清洗学习节奏',
       '本周完成“缺失值与数据清洗”剩余内容；记录一个仍未理解的概念，并在行动卡中提交学习反馈。',
       'PENDING', NOW(), NOW()
WHERE @learning_class_id IS NOT NULL
  AND @learning_student_id IS NOT NULL
  AND @learning_teacher_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM edu_learning_intervention
      WHERE class_id = @learning_class_id
        AND course_id = 91001
        AND student_id = @learning_student_id
        AND title = '补齐数据清洗学习节奏'
  );

INSERT INTO edu_learning_intervention
    (class_id, course_id, student_id, teacher_id, risk_score, title, task_description, status, student_feedback, created_at, updated_at)
SELECT @learning_class_id, 91002, @learning_student_id, @learning_teacher_id, 28,
       '复盘训练集与验证集的区别',
       '用自己的话写出训练集和验证集分别解决什么问题，再提交给教师复核。',
       'READY_FOR_REVIEW', '已完成复盘：训练集用于拟合模型，验证集用于检查模型是否具备泛化能力。',
       DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @learning_class_id IS NOT NULL
  AND @learning_student_id IS NOT NULL
  AND @learning_teacher_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM edu_learning_intervention
      WHERE class_id = @learning_class_id
        AND course_id = 91002
        AND student_id = @learning_student_id
        AND title = '复盘训练集与验证集的区别'
  );

INSERT INTO edu_learning_intervention
    (class_id, course_id, student_id, teacher_id, risk_score, title, task_description, status, created_at, updated_at)
SELECT @learning_demo_class_id, 91001, @learning_demo_student_id, @learning_demo_teacher_id, 82,
       '补齐数据清洗学习节奏',
       '本周完成“缺失值与数据清洗”剩余内容；记录一个仍未理解的概念，并在行动卡中提交学习反馈。',
       'PENDING', NOW(), NOW()
WHERE @learning_demo_class_id IS NOT NULL
  AND @learning_demo_student_id IS NOT NULL
  AND @learning_demo_teacher_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM edu_learning_intervention
      WHERE class_id = @learning_demo_class_id
        AND course_id = 91001
        AND student_id = @learning_demo_student_id
        AND title = '补齐数据清洗学习节奏'
  );

INSERT INTO edu_learning_intervention
    (class_id, course_id, student_id, teacher_id, risk_score, title, task_description, status, student_feedback, created_at, updated_at)
SELECT @learning_demo_class_id, 91002, @learning_demo_student_id, @learning_demo_teacher_id, 28,
       '复盘训练集与验证集的区别',
       '用自己的话写出训练集和验证集分别解决什么问题，再提交给教师复核。',
       'READY_FOR_REVIEW', '已完成复盘：训练集用于拟合模型，验证集用于检查模型是否具备泛化能力。',
       DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @learning_demo_class_id IS NOT NULL
  AND @learning_demo_student_id IS NOT NULL
  AND @learning_demo_teacher_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM edu_learning_intervention
      WHERE class_id = @learning_demo_class_id
        AND course_id = 91002
        AND student_id = @learning_demo_student_id
        AND title = '复盘训练集与验证集的区别'
  );

-- 导入后应返回 1 个班级、2 门课程、5 条学习记录和 2 张行动卡。
SELECT
    (SELECT COUNT(*) FROM edu_course_class WHERE class_id = @learning_class_id AND course_id IN (91001, 91002)) AS assigned_courses,
    (SELECT COUNT(*) FROM edu_study_record WHERE student_id = @learning_student_id AND course_id IN (91001, 91002)) AS study_records,
    (SELECT COUNT(*) FROM edu_learning_intervention WHERE student_id = @learning_student_id AND course_id IN (91001, 91002)) AS interventions,
    (SELECT COUNT(*) FROM edu_class_student WHERE class_id = @learning_demo_class_id AND student_id = @learning_demo_student_id) AS demo_class_members,
    (SELECT COUNT(*) FROM edu_learning_intervention WHERE student_id = @learning_demo_student_id AND course_id IN (91001, 91002)) AS demo_interventions;

-- AI 学习诊断与成长闭环数据。所有案例均关联上方已落库的班级、课程、章节和学习记录，
-- 可以重复执行，不会覆盖教师或学生后续真实产生的学情案例。
-- 如果旧版脚本曾在 Windows 默认字符集下导入，这里会校正本模块演示数据的中文展示。
UPDATE sys_user SET real_name = '学情演示教师', grade = '高中', school = '星河实验中学' WHERE id = 92001 AND username = 'learning_teacher';
UPDATE sys_user SET real_name = '学情演示学生', grade = '高中', school = '星河实验中学' WHERE id = 92002 AND username = 'learning_student';
UPDATE edu_class SET class_name = '学情分析演示班', grade = '高中', school = '星河实验中学' WHERE id = 92001 AND class_code = 'LEARN2026';
UPDATE edu_course SET course_name = 'Python 数据分析基础', intro = '围绕数据读取、清洗、可视化和结论表达，完成一个可复现的数据分析任务。' WHERE id = 91001;
UPDATE edu_course SET course_name = '机器学习入门', intro = '理解监督学习、训练集与验证集、分类特征和模型评估的完整流程。' WHERE id = 91002;
UPDATE edu_chapter SET chapter_name = '数据表与变量类型' WHERE id = 91101;
UPDATE edu_chapter SET chapter_name = '缺失值与数据清洗' WHERE id = 91102;
UPDATE edu_chapter SET chapter_name = '分组统计与可视化' WHERE id = 91103;
UPDATE edu_chapter SET chapter_name = '数据分析小项目' WHERE id = 91104;
UPDATE edu_chapter SET chapter_name = '监督学习的基本问题' WHERE id = 91201;
UPDATE edu_chapter SET chapter_name = '训练集与验证集' WHERE id = 91202;
UPDATE edu_chapter SET chapter_name = '分类模型与特征' WHERE id = 91203;
UPDATE edu_chapter SET chapter_name = '模型评估与复盘' WHERE id = 91204;
CREATE TABLE IF NOT EXISTS edu_learning_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL, course_id BIGINT NOT NULL, chapter_id BIGINT NULL,
    student_id BIGINT NOT NULL, teacher_id BIGINT NOT NULL,
    risk_score INT NOT NULL DEFAULT 0, risk_level VARCHAR(20) NOT NULL,
    behavior_snapshot TEXT NOT NULL, diagnosis VARCHAR(1000) NOT NULL,
    diagnosis_source VARCHAR(30) NOT NULL, model_name VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_learning_case_class (class_id), INDEX idx_learning_case_student (student_id), INDEX idx_learning_case_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI学习诊断案例';

CREATE TABLE IF NOT EXISTS edu_learning_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, case_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL, learning_goal VARCHAR(1000) NOT NULL, task_steps TEXT NOT NULL,
    duration_minutes INT NOT NULL, acceptance_criteria VARCHAR(1000) NOT NULL, check_question VARCHAR(1000) NOT NULL,
    expected_signals TEXT NOT NULL, teacher_decision VARCHAR(30) NOT NULL DEFAULT 'PENDING', status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_learning_plan_case (case_id), INDEX idx_learning_plan_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI学习微计划';

CREATE TABLE IF NOT EXISTS edu_learning_evidence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, plan_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
    reflection VARCHAR(2000) NOT NULL, difficulty VARCHAR(1000) NOT NULL, answer VARCHAR(2000) NOT NULL,
    ai_assessment VARCHAR(1000) NOT NULL, confidence INT NOT NULL DEFAULT 0, result VARCHAR(30) NOT NULL,
    assessment_source VARCHAR(30) NOT NULL, teacher_conclusion VARCHAR(1000) NULL,
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, reviewed_at DATETIME NULL,
    INDEX idx_learning_evidence_plan (plan_id), INDEX idx_learning_evidence_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生学习证据与理解检查';

CREATE TABLE IF NOT EXISTS edu_learning_ai_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, case_id BIGINT NULL, plan_id BIGINT NULL, student_id BIGINT NOT NULL,
    operation VARCHAR(30) NOT NULL, model_name VARCHAR(120) NOT NULL, source VARCHAR(30) NOT NULL,
    context_summary VARCHAR(1000) NOT NULL, elapsed_millis BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_learning_trace_case (case_id), INDEX idx_learning_trace_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学情AI生成轨迹';

INSERT INTO edu_learning_case
    (class_id, course_id, chapter_id, student_id, teacher_id, risk_score, risk_level, behavior_snapshot, diagnosis, diagnosis_source, model_name, status, created_at, updated_at)
SELECT @learning_demo_class_id, 91001, 91102, @learning_demo_student_id, @learning_demo_teacher_id, 82, 'HIGH',
       JSON_OBJECT('progress', 36, 'studyMinutes', 58, 'finishedChapters', 1, 'courseAverage', 36, 'idleDays', 10, 'lastStudyTime', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 10 DAY), '%Y-%m-%d %H:%i')),
       '该学生在“Python 数据分析基础”中已停留 10 天，当前仅完成第一个章节，且距离课程截止时间较近。先恢复一次短时学习，并用概念解释验证是否真正进入下一章节。',
       'FALLBACK', 'course-data-fallback', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @learning_demo_class_id IS NOT NULL AND @learning_demo_student_id IS NOT NULL AND @learning_demo_teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_learning_case WHERE class_id = @learning_demo_class_id AND course_id = 91001 AND student_id = @learning_demo_student_id AND status IN ('PUBLISHED', 'CONTINUE', 'EVIDENCE_SUBMITTED'));

SET @growth_active_case_id := (
    SELECT id FROM edu_learning_case WHERE class_id = @learning_demo_class_id AND course_id = 91001
      AND student_id = @learning_demo_student_id AND status IN ('PUBLISHED', 'CONTINUE', 'EVIDENCE_SUBMITTED')
    ORDER BY id DESC LIMIT 1
);

INSERT INTO edu_learning_plan
    (case_id, title, learning_goal, task_steps, duration_minutes, acceptance_criteria, check_question, expected_signals, teacher_decision, status, created_at, updated_at)
SELECT @growth_active_case_id,
       '完成“缺失值与数据清洗”微计划',
       '在缺失值与数据清洗章节完成一个可验证的小步骤，恢复连续学习节奏。',
       JSON_ARRAY('进入“缺失值与数据清洗”，连续学习 15 分钟并完成一个小节。', '用自己的话写下缺失值处理的一个方法及其适用情况。', '完成理解检查并如实记录仍不确定的地方。'),
       25,
       '课程学习记录新增时长或进度；提交缺失值处理方法的解释和理解检查回答。',
       '请用自己的话说明：为什么不能在不了解数据含义时直接删除所有缺失值？',
       JSON_ARRAY('学习间隔缩短', '本课程新增学习时长', '能够说明一种缺失值处理方式'),
       'ADOPTED', 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @growth_active_case_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_learning_plan WHERE case_id = @growth_active_case_id);

INSERT INTO edu_learning_ai_trace (case_id, plan_id, student_id, operation, model_name, source, context_summary, elapsed_millis, created_at)
SELECT @growth_active_case_id,
       (SELECT id FROM edu_learning_plan WHERE case_id = @growth_active_case_id ORDER BY id DESC LIMIT 1),
       @learning_demo_student_id, 'PLAN_GENERATION', 'course-data-fallback', 'FALLBACK',
       'Python 数据分析基础 / 缺失值与数据清洗 / 真实学习记录：进度36%，距上次学习10天', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @growth_active_case_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_learning_ai_trace WHERE case_id = @growth_active_case_id AND operation = 'PLAN_GENERATION');

INSERT INTO edu_learning_case
    (class_id, course_id, chapter_id, student_id, teacher_id, risk_score, risk_level, behavior_snapshot, diagnosis, diagnosis_source, model_name, status, created_at, updated_at)
SELECT @learning_demo_class_id, 91002, 91203, @learning_demo_student_id, @learning_demo_teacher_id, 28, 'LOW',
       JSON_OBJECT('progress', 68, 'studyMinutes', 120, 'finishedChapters', 2, 'courseAverage', 68, 'idleDays', 1, 'lastStudyTime', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 DAY), '%Y-%m-%d %H:%i')),
       '学生保持了连续学习记录。通过一次训练集与验证集的概念复盘，确认已经能够区分两者在模型学习流程中的作用。',
       'FALLBACK', 'course-data-fallback', 'EFFECTIVE', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE @learning_demo_class_id IS NOT NULL AND @learning_demo_student_id IS NOT NULL AND @learning_demo_teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_learning_case WHERE class_id = @learning_demo_class_id AND course_id = 91002 AND student_id = @learning_demo_student_id AND status = 'EFFECTIVE');

SET @growth_effective_case_id := (
    SELECT id FROM edu_learning_case WHERE class_id = @learning_demo_class_id AND course_id = 91002
      AND student_id = @learning_demo_student_id AND status = 'EFFECTIVE' ORDER BY id DESC LIMIT 1
);

INSERT INTO edu_learning_plan
    (case_id, title, learning_goal, task_steps, duration_minutes, acceptance_criteria, check_question, expected_signals, teacher_decision, status, created_at, updated_at)
SELECT @growth_effective_case_id,
       '复盘训练集与验证集的作用',
       '能够说明训练集和验证集分别服务于模型学习流程的哪一步。',
       JSON_ARRAY('复习训练集、验证集的定义。', '用自己的话写出两者分别解决的问题。', '回答理解检查问题。'),
       20,
       '能准确说出训练集用于拟合，验证集用于检查模型泛化表现。',
       '训练集和验证集分别在模型学习流程中解决什么问题？',
       JSON_ARRAY('完成章节复习', '能够区分训练与验证的用途'),
       'ADOPTED', 'REVIEWED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE @growth_effective_case_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_learning_plan WHERE case_id = @growth_effective_case_id);

SET @growth_effective_plan_id := (SELECT id FROM edu_learning_plan WHERE case_id = @growth_effective_case_id ORDER BY id DESC LIMIT 1);

INSERT INTO edu_learning_evidence
    (plan_id, student_id, reflection, difficulty, answer, ai_assessment, confidence, result, assessment_source, teacher_conclusion, submitted_at, reviewed_at)
SELECT @growth_effective_plan_id, @learning_demo_student_id,
       '完成了训练集与验证集章节复习，并整理了两者在模型训练中的不同作用。',
       '一开始把验证集和测试集混淆，复习后能够区分。',
       '训练集用来让模型从样本中拟合规律；验证集不参与拟合，用来观察模型对新数据的表现，并帮助调整模型。',
       '学生能够区分训练集用于拟合、验证集用于检查泛化表现，回答与课程学习目标一致。', 86, 'MASTERED', 'TEACHER_REVIEW',
       '本轮目标达成。后续学习模型评估时继续区分验证集与最终测试集。', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE @growth_effective_plan_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_learning_evidence WHERE plan_id = @growth_effective_plan_id);

SELECT
    (SELECT COUNT(*) FROM edu_learning_case WHERE student_id = @learning_demo_student_id) AS learning_cases,
    (SELECT COUNT(*) FROM edu_learning_plan p JOIN edu_learning_case c ON c.id = p.case_id WHERE c.student_id = @learning_demo_student_id) AS learning_plans,
    (SELECT COUNT(*) FROM edu_learning_evidence WHERE student_id = @learning_demo_student_id) AS learning_evidence,
    (SELECT COUNT(*) FROM edu_learning_ai_trace WHERE student_id = @learning_demo_student_id) AS ai_traces;

-- 学习画像与AI课程推荐演示数据：所有占比均由下方真实学习时长汇总得到。
-- 课程类型约定：1 理论课、2 项目实践课、3 实验课。
UPDATE edu_course SET course_type = 2 WHERE id = 91001;
UPDATE edu_course SET course_type = 1 WHERE id = 91002;

INSERT INTO edu_course
    (id, course_name, grade, difficulty, course_type, teacher_id, intro, total_duration, total_chapter, is_public, status, create_by, update_by)
SELECT 91003, '计算机视觉实验', '高中', 2, 3, @learning_demo_teacher_id,
       '通过图像特征、分类实验和结果复盘，完成一个可验证的计算机视觉实验。',
       120, 3, 0, 1, @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE @learning_demo_teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_course WHERE id = 91003);

INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91301, 91003, '图像与像素表示', 1, 35, @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91003)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91301);
INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91302, 91003, '图像分类小实验', 2, 45, @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91003)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91302);
INSERT INTO edu_chapter (id, course_id, chapter_name, sort, duration, create_by, update_by)
SELECT 91303, 91003, '实验结果复盘', 3, 40, @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE EXISTS (SELECT 1 FROM edu_course WHERE id = 91003)
  AND NOT EXISTS (SELECT 1 FROM edu_chapter WHERE id = 91303);

INSERT INTO edu_course_class (course_id, class_id, publish_time, deadline)
SELECT 91003, @learning_demo_class_id, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY)
WHERE @learning_demo_class_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_course_class WHERE course_id = 91003 AND class_id = @learning_demo_class_id);

-- 为教师侧学生对比增加两名仅用于本地验收的演示学生。
INSERT INTO sys_user
    (id, username, password, real_name, user_type, grade, school, status, create_by, update_by)
SELECT 92003, 'learning_profile_a', '$2a$10$fPLyrSvzxqgYAZi7t48j5u/BUvXScaTdytk1nbE80FhRyUQPcE4Hi',
       '画像演示学生甲', 1, '高中', '星河实验中学', 1, @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE @learning_demo_teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 92003);
INSERT INTO sys_user
    (id, username, password, real_name, user_type, grade, school, status, create_by, update_by)
SELECT 92004, 'learning_profile_b', '$2a$10$fPLyrSvzxqgYAZi7t48j5u/BUvXScaTdytk1nbE80FhRyUQPcE4Hi',
       '画像演示学生乙', 1, '高中', '星河实验中学', 1, @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE @learning_demo_teacher_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 92004);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 92003, @student_role_id WHERE @student_role_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 92003 AND role_id = @student_role_id);
INSERT INTO sys_user_role (user_id, role_id)
SELECT 92004, @student_role_id WHERE @student_role_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 92004 AND role_id = @student_role_id);
INSERT INTO edu_class_student (class_id, student_id, join_time)
SELECT @learning_demo_class_id, 92003, DATE_SUB(NOW(), INTERVAL 19 DAY)
WHERE @learning_demo_class_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_class_student WHERE class_id = @learning_demo_class_id AND student_id = 92003);
INSERT INTO edu_class_student (class_id, student_id, join_time)
SELECT @learning_demo_class_id, 92004, DATE_SUB(NOW(), INTERVAL 18 DAY)
WHERE @learning_demo_class_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_class_student WHERE class_id = @learning_demo_class_id AND student_id = 92004);
UPDATE edu_class SET student_count = 3 WHERE id = @learning_demo_class_id;

INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_demo_student_id, 91003, 91301, 100, 30, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_demo_student_id AND chapter_id = 91301);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT @learning_demo_student_id, 91003, 91302, 40, 14, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = @learning_demo_student_id AND chapter_id = 91302);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT 92003, 91001, 91101, 100, 50, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = 92003 AND chapter_id = 91101);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT 92003, 91002, 91201, 100, 75, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = 92003 AND chapter_id = 91201);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT 92003, 91003, 91301, 45, 18, 0, DATE_SUB(NOW(), INTERVAL 3 DAY)
WHERE NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = 92003 AND chapter_id = 91301);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT 92004, 91001, 91101, 100, 90, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = 92004 AND chapter_id = 91101);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT 92004, 91002, 91201, 45, 20, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)
WHERE NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = 92004 AND chapter_id = 91201);
INSERT INTO edu_study_record (student_id, course_id, chapter_id, progress, study_duration, finish_status, last_study_time)
SELECT 92004, 91003, 91301, 100, 65, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM edu_study_record WHERE student_id = 92004 AND chapter_id = 91301);

-- 公开候选课程只供AI推荐和课程库跳转使用；学生端不会伪造任何不存在的推荐。
INSERT INTO edu_course
    (id, course_name, grade, difficulty, course_type, teacher_id, intro, total_duration, total_chapter, is_public, status, create_by, update_by)
SELECT 91011, '机器学习项目实战', '高中', 2, 2, @learning_demo_teacher_id,
       '用真实任务串联数据准备、特征设计、模型训练和结果表达。',
       180, 4, 1, 1, @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE @learning_demo_teacher_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM edu_course WHERE id = 91011);
INSERT INTO edu_course
    (id, course_name, grade, difficulty, course_type, teacher_id, intro, total_duration, total_chapter, is_public, status, create_by, update_by)
SELECT 91012, '计算机视觉入门实验', '高中', 1, 3, @learning_demo_teacher_id,
       '通过图像分类和误差分析完成一次轻量级视觉实验。',
       120, 3, 1, 1, @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE @learning_demo_teacher_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM edu_course WHERE id = 91012);
INSERT INTO edu_course
    (id, course_name, grade, difficulty, course_type, teacher_id, intro, total_duration, total_chapter, is_public, status, create_by, update_by)
SELECT 91013, 'AI伦理与数据安全', '高中', 1, 1, @learning_demo_teacher_id,
       '理解数据使用边界、隐私保护和人工智能应用中的责任问题。',
       90, 3, 1, 1, @learning_demo_teacher_id, @learning_demo_teacher_id
WHERE @learning_demo_teacher_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM edu_course WHERE id = 91013);

CREATE TABLE IF NOT EXISTS edu_learning_recommendation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id VARCHAR(64) NOT NULL,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    recommendation_score INT NOT NULL DEFAULT 0,
    reason VARCHAR(1000) NOT NULL,
    source VARCHAR(30) NOT NULL,
    model_name VARCHAR(120) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_learning_recommendation_student (student_id),
    INDEX idx_learning_recommendation_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI课程推荐结果';

INSERT INTO edu_learning_recommendation
    (batch_id, student_id, course_id, recommendation_score, reason, source, model_name, created_at)
SELECT 'seed-profile-20260729', @learning_demo_student_id, 91011, 88,
       '你已在机器学习理论课中形成稳定学习记录，项目实战课可把训练集、特征和模型评估串成完整任务。',
       'FALLBACK', 'course-data-fallback', NOW()
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_learning_recommendation WHERE student_id = @learning_demo_student_id AND course_id = 91011);
INSERT INTO edu_learning_recommendation
    (batch_id, student_id, course_id, recommendation_score, reason, source, model_name, created_at)
SELECT 'seed-profile-20260729', @learning_demo_student_id, 91012, 81,
       '你已有实验课的图像学习记录，这门入门实验可延续动手学习节奏，并补全图像分类的完整过程。',
       'FALLBACK', 'course-data-fallback', NOW()
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_learning_recommendation WHERE student_id = @learning_demo_student_id AND course_id = 91012);
INSERT INTO edu_learning_recommendation
    (batch_id, student_id, course_id, recommendation_score, reason, source, model_name, created_at)
SELECT 'seed-profile-20260729', @learning_demo_student_id, 91013, 74,
       '这门理论课能补充人工智能应用中的数据安全视角，和现有数据分析课程形成更完整的学习路径。',
       'FALLBACK', 'course-data-fallback', NOW()
WHERE @learning_demo_student_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM edu_learning_recommendation WHERE student_id = @learning_demo_student_id AND course_id = 91013);

-- Keep the module-owned demo text idempotent so rerunning this script also repairs
-- a partial import made with a non-UTF-8 terminal encoding.
UPDATE sys_user
SET real_name = CASE username
    WHEN 'learning_teacher' THEN '学情演示教师'
    WHEN 'learning_student' THEN '学情演示学生'
    WHEN 'learning_profile_a' THEN '画像演示学生甲'
    WHEN 'learning_profile_b' THEN '画像演示学生乙'
END
WHERE username IN ('learning_teacher', 'learning_student', 'learning_profile_a', 'learning_profile_b');

UPDATE edu_class
SET class_name = '学情分析演示班'
WHERE class_code = 'LEARN2026';

UPDATE edu_course
SET course_name = CASE id
        WHEN 91001 THEN 'Python 数据分析基础'
        WHEN 91002 THEN '机器学习入门'
        WHEN 91003 THEN '计算机视觉实验'
        WHEN 91011 THEN '机器学习项目实战'
        WHEN 91012 THEN '计算机视觉入门实验'
        WHEN 91013 THEN 'AI 伦理与数据安全'
    END,
    intro = CASE id
        WHEN 91001 THEN '围绕数据读取、清洗、可视化和结论表达，完成一个可复现的数据分析任务。'
        WHEN 91002 THEN '理解监督学习、训练集与验证集、分类特征和模型评估的完整流程。'
        WHEN 91003 THEN '通过图像特征、分类实验和结果复盘，完成一个可验证的计算机视觉实验。'
        WHEN 91011 THEN '用真实任务串联数据准备、特征设计、模型训练和结果表达。'
        WHEN 91012 THEN '通过图像分类和误差分析完成一次轻量级视觉实验。'
        WHEN 91013 THEN '理解数据使用边界、隐私保护和人工智能应用中的责任问题。'
    END,
    ext_json = CASE id
        WHEN 91001 THEN '{"learningCategory":"数据分析","tags":["数据分析","Python"]}'
        WHEN 91002 THEN '{"learningCategory":"机器学习","tags":["机器学习","监督学习"]}'
        WHEN 91003 THEN '{"learningCategory":"计算机视觉","tags":["计算机视觉","图像分类"]}'
        WHEN 91011 THEN '{"learningCategory":"机器学习","tags":["机器学习","项目实战"]}'
        WHEN 91012 THEN '{"learningCategory":"计算机视觉","tags":["计算机视觉","图像分类"]}'
        WHEN 91013 THEN '{"learningCategory":"AI 伦理与安全","tags":["AI 伦理与安全","数据安全"]}'
    END
WHERE id IN (91001, 91002, 91003, 91011, 91012, 91013);

UPDATE edu_chapter
SET chapter_name = CASE id
        WHEN 91101 THEN '数据表与变量类型'
        WHEN 91102 THEN '缺失值与数据清洗'
        WHEN 91103 THEN '分组统计与可视化'
        WHEN 91104 THEN '数据分析小项目'
        WHEN 91201 THEN '监督学习的基本问题'
        WHEN 91202 THEN '训练集与验证集'
        WHEN 91203 THEN '分类模型与特征'
        WHEN 91204 THEN '模型评估与复盘'
        WHEN 91301 THEN '图像与像素表示'
        WHEN 91302 THEN '图像分类小实验'
        WHEN 91303 THEN '实验结果复盘'
    END
WHERE id IN (91101, 91102, 91103, 91104, 91201, 91202, 91203, 91204, 91301, 91302, 91303);

UPDATE edu_learning_recommendation
SET reason = CASE course_id
        WHEN 91011 THEN '你近期投入较多的是机器学习主题，这门项目实战课程可把训练集、特征和模型评估串成完整任务。'
        WHEN 91012 THEN '这门课程延续计算机视觉主题，能把图像分类和误差分析串成完整的动手练习。'
        WHEN 91013 THEN '这门课程补充 AI 伦理与安全主题，让现有的数据分析学习路径覆盖数据使用边界与隐私保护。'
    END
WHERE student_id = 92002 AND course_id IN (91011, 91012, 91013);
