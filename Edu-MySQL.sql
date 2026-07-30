-- 创建数据库edu，不存在则创建
CREATE
DATABASE IF NOT EXISTS edu DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
-- 使用edu数据库
USE
edu;

-- 一、系统权限模块
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    password        VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码，SpringSecurity专用',
    real_name       VARCHAR(30)  NOT NULL COMMENT '真实姓名',
    phone           VARCHAR(11) COMMENT '手机号',
    email           VARCHAR(100) COMMENT '邮箱',
    avatar          VARCHAR(255) COMMENT '头像地址',
    user_type       TINYINT      NOT NULL COMMENT '用户类型 1学生 2教师 3教研人员 4平台管理员 5超级管理员',
    grade           VARCHAR(20) COMMENT '学段：小学/初中/高中，学生必填',
    school          VARCHAR(100) COMMENT '学校名称',
    status          TINYINT       DEFAULT 1 COMMENT '账号状态 0禁用 1正常',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50) COMMENT '最后登录IP',
    create_by       BIGINT COMMENT '创建人ID',
    update_by       BIGINT COMMENT '更新人ID',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    ext_json        VARCHAR(2000) DEFAULT '{}',
    INDEX idx_username (username),
    INDEX idx_user_type (user_type)
) COMMENT '系统用户表';

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50) NOT NULL UNIQUE COMMENT '角色标识：STUDENT/TEACHER/RESEARCH/ADMIN/SUPERADMIN',
    sort        INT           DEFAULT 0 COMMENT '排序',
    remark      VARCHAR(500) COMMENT '角色备注',
    create_by   BIGINT,
    update_by   BIGINT,
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    ext_json    VARCHAR(2000) DEFAULT '{}'
) COMMENT '角色表';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) COMMENT '用户角色关联';

-- 二、班级教学管理模块
DROP TABLE IF EXISTS edu_class;
CREATE TABLE edu_class
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_name    VARCHAR(100)       NOT NULL COMMENT '班级名称：高一AI创新1班',
    teacher_id    BIGINT             NOT NULL COMMENT '班主任教师ID(sys_user.id)',
    grade         VARCHAR(20)        NOT NULL COMMENT '学段 小学/初中/高中',
    school        VARCHAR(100)       NOT NULL COMMENT '所属学校',
    class_code    VARCHAR(30) UNIQUE NOT NULL COMMENT '班级加入邀请码',
    join_type     TINYINT            NOT NULL DEFAULT 1 COMMENT '加入方式：1仅邀请码加入 2公开可直接加入',
    student_count INT                         DEFAULT 0 COMMENT '当前学生人数',
    status        TINYINT                     DEFAULT 1 COMMENT '0归档 1正常',
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   DATETIME                    DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME                    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT                     DEFAULT 0,
    ext_json      VARCHAR(2000)               DEFAULT '{}',
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_class_code (class_code)
) COMMENT '教学班级';

DROP TABLE IF EXISTS edu_class_student;
CREATE TABLE edu_class_student
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id   BIGINT NOT NULL COMMENT '班级ID',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    join_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入班时间',
    UNIQUE KEY uk_class_student (class_id, student_id),
    INDEX idx_class_id (class_id),
    INDEX idx_student_id (student_id)
) COMMENT '班级学生关联';

DROP TABLE IF EXISTS edu_course_class;
CREATE TABLE edu_course_class
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id    BIGINT NOT NULL COMMENT '课程ID',
    class_id     BIGINT NOT NULL COMMENT '班级ID',
    publish_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
    deadline     DATETIME COMMENT '完成截止时间',
    UNIQUE KEY uk_course_class (course_id, class_id),
    INDEX idx_class_id (class_id)
) COMMENT '课程下发班级';

-- 三、课程 & 学习进度模块
DROP TABLE IF EXISTS edu_course;
CREATE TABLE edu_course
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_name    VARCHAR(200) NOT NULL COMMENT '课程名称',
    cover          VARCHAR(255) COMMENT '封面图链接URL',
    grade          VARCHAR(20)  NOT NULL COMMENT '适配学段',
    difficulty     TINYINT      NOT NULL COMMENT '难度 1入门 2进阶 3高阶',
    course_type    TINYINT      NOT NULL COMMENT '1理论课 2项目实践课 3实验课',
    teacher_id     BIGINT COMMENT '创建教师ID，平台公共课程填管理员ID',
    intro          TEXT COMMENT '课程简介',
    total_duration INT           DEFAULT 0 COMMENT '总时长分钟',
    total_chapter  INT           DEFAULT 0 COMMENT '总章节数',
    is_public      TINYINT       DEFAULT 0 COMMENT '0私有 1平台公开',
    status         TINYINT       DEFAULT 1 COMMENT '0草稿 1已发布 2下架',
    create_by      BIGINT,
    update_by      BIGINT,
    create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT       DEFAULT 0,
    ext_json       VARCHAR(2000) DEFAULT '{}',
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_grade (grade),
    INDEX idx_course_type (course_type)
) COMMENT 'AI课程主表';

DROP TABLE IF EXISTS edu_chapter;
CREATE TABLE edu_chapter
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id    BIGINT       NOT NULL COMMENT '所属课程',
    chapter_name VARCHAR(200) NOT NULL COMMENT '章节名',
    sort         INT           DEFAULT 0 COMMENT '排序',
    duration     INT           DEFAULT 0 COMMENT '章节时长',
    create_by    BIGINT,
    update_by    BIGINT,
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT       DEFAULT 0,
    ext_json     VARCHAR(2000) DEFAULT '{}',
    INDEX idx_course_id (course_id)
) COMMENT '课程章节';

DROP TABLE IF EXISTS edu_resource;
CREATE TABLE edu_resource
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    chapter_id    BIGINT       NOT NULL COMMENT '所属章节',
    resource_name VARCHAR(200) NOT NULL COMMENT '资源名称',
    resource_type TINYINT      NOT NULL COMMENT '1视频 2PDF文档 3图片素材 4数据集',
    resource_url  VARCHAR(255) NOT NULL COMMENT '文件存储地址',
    file_size     BIGINT COMMENT '文件大小byte',
    duration      INT           DEFAULT 0 COMMENT '视频时长',
    sort          INT           DEFAULT 0,
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT       DEFAULT 0,
    ext_json      VARCHAR(2000) DEFAULT '{}',
    INDEX idx_chapter_id (chapter_id)
) COMMENT '课程配套学习资源';

DROP TABLE IF EXISTS edu_study_record;
CREATE TABLE edu_study_record
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id      BIGINT NOT NULL COMMENT '学生ID',
    course_id       BIGINT NOT NULL COMMENT '课程ID',
    chapter_id      BIGINT NOT NULL COMMENT '章节ID',
    resource_id     BIGINT COMMENT '资源ID',
    progress        INT      DEFAULT 0 COMMENT '观看进度百分比',
    study_duration  INT      DEFAULT 0 COMMENT '本次学习时长分钟',
    finish_status   TINYINT  DEFAULT 0 COMMENT '0未完成 1已完成',
    last_study_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stu_chapter (student_id, chapter_id),
    INDEX idx_student_id (student_id),
    INDEX idx_course_id (course_id)
) COMMENT '学生学习进度记录';

DROP TABLE IF EXISTS edu_safety_record;
CREATE TABLE edu_safety_record
(
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_module         VARCHAR(50)  NOT NULL COMMENT '来源模块',
    scene                 VARCHAR(50)  NOT NULL COMMENT '安全场景',
    user_role             VARCHAR(20)  NOT NULL COMMENT '用户角色',
    grade_level           VARCHAR(20)  NOT NULL COMMENT '学段',
    user_id               BIGINT COMMENT '用户ID',
    class_id              BIGINT COMMENT '班级ID',
    course_id             BIGINT COMMENT '课程ID',
    chapter_id            BIGINT COMMENT '章节ID',
    input_text            TEXT COMMENT '输入文本',
    output_text           TEXT COMMENT '输出文本',
    allowed               TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否放行',
    risk_level            VARCHAR(20)  NOT NULL COMMENT '风险等级',
    risk_types            TEXT COMMENT '风险类型列表(JSON)',
    decision              VARCHAR(20)  NOT NULL COMMENT '处置动作',
    reason                TEXT COMMENT '处置原因',
    suggestion            TEXT COMMENT '处置建议',
    processed_text        TEXT COMMENT '处理后文本',
    evidence_level        VARCHAR(20)  COMMENT 'RAG证据级别',
    evidence_score        DOUBLE       COMMENT 'RAG证据分值',
    manual_review_required TINYINT(1)   DEFAULT 0 COMMENT '是否需要人工复审',
    review_status         VARCHAR(20)  DEFAULT 'NOT_REQUIRED' COMMENT '复审状态',
    review_by             BIGINT       COMMENT '复审人ID',
    review_by_name        VARCHAR(50)  COMMENT '复审人姓名',
    review_time           DATETIME     COMMENT '复审时间',
    review_comment        TEXT         COMMENT '复审备注',
    metadata_json         TEXT COMMENT '请求元信息(JSON)',
    debug_json            TEXT COMMENT '调试信息(JSON)',
    create_time           DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_create_time (create_time),
    INDEX idx_risk_level (risk_level),
    INDEX idx_source_module (source_module),
    INDEX idx_scene (scene),
    INDEX idx_manual_review_required (manual_review_required)
) COMMENT '教育安全检测记录';

-- 四、初始化角色与测试账号
INSERT IGNORE INTO sys_role (role_name, role_code, sort, remark, deleted, ext_json)
VALUES
    ('学生', 'STUDENT', 1, '学生角色', 0, '{}'),
    ('教师', 'TEACHER', 2, '教师角色', 0, '{}'),
    ('教研人员', 'RESEARCH', 3, '教研角色', 0, '{}'),
    ('平台管理员', 'ADMIN', 4, '平台管理员角色', 0, '{}'),
    ('超级管理员', 'SUPERADMIN', 5, '超级管理员角色', 0, '{}');

INSERT IGNORE INTO sys_user
(username, password, real_name, phone, email, avatar, user_type, grade, school, status, deleted, ext_json)
VALUES
    ('admin001', '$2a$10$h8U0bLZx.PnPeKCfDa3YzOqlniYLVnocgDgpZJ16oPk1Rkmu3kKTe', '平台管理员', NULL, 'admin001@edu-b.com', NULL, 4, NULL, 'Edu-B平台', 1, 0, '{}'),
    ('teacher001', '$2a$10$E9fSLk9CuGoqX2Y/TbU22e9zed6BT2L5aoLl3beELf4Y9rU0/mQXi', '张老师', NULL, 'teacher001@edu-b.com', NULL, 2, '高中', 'Edu-B示例学校', 1, 0, '{}');

SET @admin_user_id := (SELECT id FROM sys_user WHERE username = 'admin001' LIMIT 1);
SET @teacher_user_id := (SELECT id FROM sys_user WHERE username = 'teacher001' LIMIT 1);
SET @admin_role_id := (SELECT id FROM sys_role WHERE role_code = 'ADMIN' LIMIT 1);
SET @teacher_role_id := (SELECT id FROM sys_role WHERE role_code = 'TEACHER' LIMIT 1);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
VALUES
    (@admin_user_id, @admin_role_id),
    (@teacher_user_id, @teacher_role_id);
