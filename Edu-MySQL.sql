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

-- 智能学伴会话与消息
CREATE TABLE IF NOT EXISTS edu_ai_companion_session
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id        BIGINT       NOT NULL COMMENT '学生用户ID',
    course_id         BIGINT       NOT NULL COMMENT '当前课程ID',
    chapter_id        BIGINT COMMENT '创建会话时所在章节ID',
    title             VARCHAR(100) NOT NULL COMMENT '会话标题',
    last_message_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近消息时间',
    create_time       DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT               DEFAULT 0,
    INDEX idx_ai_session_student (student_id, last_message_time),
    INDEX idx_ai_session_course (course_id)
) COMMENT '学生智能学伴会话';

CREATE TABLE IF NOT EXISTS edu_ai_companion_message
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id  BIGINT      NOT NULL COMMENT '会话ID',
    student_id  BIGINT      NOT NULL COMMENT '学生用户ID',
    role        VARCHAR(20) NOT NULL COMMENT '消息角色 USER/ASSISTANT',
    content     TEXT        NOT NULL COMMENT '消息内容',
    chapter_id  BIGINT COMMENT '提问时所在章节ID',
    resource_id BIGINT COMMENT '提问时所在资源ID',
    generation_mode VARCHAR(20) COMMENT '生成方式 MODEL/FALLBACK',
    model_name  VARCHAR(100) COMMENT '生成回答的模型名称',
    source_summary VARCHAR(500) COMMENT '回答参考的课程章节资源',
    safety_status VARCHAR(20) COMMENT '安全状态 NORMAL/BLOCKED',
    response_time_ms BIGINT COMMENT '回答生成耗时（毫秒）',
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT              DEFAULT 0,
    INDEX idx_ai_message_session (session_id, create_time),
    INDEX idx_ai_message_student (student_id)
) COMMENT '学生智能学伴消息';
