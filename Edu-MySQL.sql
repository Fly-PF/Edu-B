-- 创建数据库edu，不存在则创建
CREATE DATABASE IF NOT EXISTS edu DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
-- 使用edu数据库
USE edu;

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
    INDEX           idx_username (username),
    INDEX           idx_user_type (user_type)
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
    INDEX       idx_user_id (user_id),
    INDEX       idx_role_id (role_id)
) COMMENT '用户角色关联';

DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT        DEFAULT 0 COMMENT '父菜单ID',
    menu_name   VARCHAR(100) NOT NULL COMMENT '菜单名称',
    menu_type   TINYINT      NOT NULL COMMENT '1目录 2菜单 3按钮/接口权限',
    permission  VARCHAR(100) COMMENT '权限标识，如：course:list,ai:chat:query',
    path        VARCHAR(255) COMMENT '前端路由地址',
    icon        VARCHAR(255) COMMENT '图标',
    sort        INT           DEFAULT 0,
    create_by   BIGINT,
    update_by   BIGINT,
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    ext_json    VARCHAR(2000) DEFAULT '{}'
) COMMENT '菜单权限表';

DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu
(
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX   idx_role_id (role_id)
) COMMENT '角色菜单关联';

DROP TABLE IF EXISTS sys_jwt_token;
CREATE TABLE sys_jwt_token
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL COMMENT '所属用户',
    token       VARCHAR(1000) NOT NULL COMMENT '完整JWT字符串',
    expire_time DATETIME      NOT NULL COMMENT '过期时间',
    status      TINYINT  DEFAULT 1 COMMENT '0失效 1有效',
    device      VARCHAR(100) COMMENT '登录设备',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX       idx_user_id (user_id),
    INDEX       idx_token (token(200))
) COMMENT 'JWT令牌存储，实现主动登出';

DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT COMMENT '操作人',
    username       VARCHAR(50) COMMENT '操作账号',
    operation      VARCHAR(200) NOT NULL COMMENT '操作描述',
    request_url    VARCHAR(255) NOT NULL COMMENT '请求地址',
    request_method VARCHAR(20) COMMENT 'GET/POST/PUT',
    ip             VARCHAR(50) COMMENT '操作IP',
    params         TEXT COMMENT '请求参数',
    cost_time      BIGINT COMMENT '耗时ms',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX          idx_user_id (user_id),
    INDEX          idx_create_time (create_time)
) COMMENT '系统操作日志';

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
    student_count INT           DEFAULT 0 COMMENT '当前学生人数',
    status        TINYINT       DEFAULT 1 COMMENT '0归档 1正常',
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT       DEFAULT 0,
    ext_json      VARCHAR(2000) DEFAULT '{}',
    INDEX         idx_teacher_id (teacher_id),
    INDEX         idx_class_code (class_code)
) COMMENT '教学班级';

DROP TABLE IF EXISTS edu_class_student;
CREATE TABLE edu_class_student
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id   BIGINT NOT NULL COMMENT '班级ID',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    join_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入班时间',
    UNIQUE KEY uk_class_student (class_id, student_id),
    INDEX      idx_class_id (class_id),
    INDEX      idx_student_id (student_id)
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
    INDEX        idx_class_id (class_id)
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
    INDEX          idx_teacher_id (teacher_id),
    INDEX          idx_grade (grade),
    INDEX          idx_course_type (course_type)
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
    INDEX        idx_course_id (course_id)
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
    INDEX         idx_chapter_id (chapter_id)
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
    INDEX           idx_student_id (student_id),
    INDEX           idx_course_id (course_id)
) COMMENT '学生学习进度记录';

-- 四、实践创作 & 在线实验模块
DROP TABLE IF EXISTS edu_project_task;
CREATE TABLE edu_project_task
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name     VARCHAR(200) NOT NULL COMMENT '项目任务名称',
    course_id     BIGINT COMMENT '绑定课程，可选',
    teacher_id    BIGINT       NOT NULL COMMENT '创建教师',
    grade         VARCHAR(20)  NOT NULL COMMENT '适配学段',
    task_type     TINYINT      NOT NULL COMMENT '1低代码积木项目 2Python代码实验 3数据标注任务 4硬件联动项目',
    demand        TEXT         NOT NULL COMMENT '任务要求',
    template_code TEXT COMMENT '初始模板代码/积木模板JSON',
    deadline      DATETIME COMMENT '提交截止时间',
    is_public     TINYINT       DEFAULT 0 COMMENT '是否公开案例',
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT       DEFAULT 0,
    ext_json      VARCHAR(2000) DEFAULT '{}',
    INDEX         idx_teacher_id (teacher_id),
    INDEX         idx_course_id (course_id)
) COMMENT 'AI实践项目任务';

DROP TABLE IF EXISTS edu_student_project;
CREATE TABLE edu_student_project
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id    BIGINT       NOT NULL COMMENT '学生',
    task_id       BIGINT       NOT NULL COMMENT '对应项目任务',
    project_name  VARCHAR(200) NOT NULL COMMENT '作品名称',
    content_json  LONGTEXT COMMENT '项目完整数据：积木配置、代码、模型参数、实验日志',
    cover         VARCHAR(255) COMMENT '作品预览封面链接URL',
    submit_status TINYINT       DEFAULT 0 COMMENT '0草稿 1已提交 2已评阅',
    submit_time   DATETIME COMMENT '提交时间',
    review_time   DATETIME COMMENT '评阅完成时间',
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT       DEFAULT 0,
    ext_json      VARCHAR(2000) DEFAULT '{}',
    INDEX         idx_student_id (student_id),
    INDEX         idx_task_id (task_id)
) COMMENT '学生创作项目作品';

DROP TABLE IF EXISTS edu_experiment_log;
CREATE TABLE edu_experiment_log
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_project_id BIGINT  NOT NULL COMMENT '关联学生项目',
    run_type           TINYINT NOT NULL COMMENT '1代码运行 2模型训练 3数据推理',
    input_params       TEXT COMMENT '运行入参',
    output_log         LONGTEXT COMMENT '控制台输出日志',
    run_status         TINYINT NOT NULL COMMENT '0运行中 1成功 2报错',
    cost_second        INT      DEFAULT 0 COMMENT '运行耗时',
    gpu_used           TINYINT  DEFAULT 0 COMMENT '是否使用GPU算力',
    create_time        DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX              idx_student_project_id (student_project_id)
) COMMENT '在线实验运行日志';

-- 五、作品展示广场模块
DROP TABLE IF EXISTS edu_project_show;
CREATE TABLE edu_project_show
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_project_id BIGINT       NOT NULL COMMENT '关联学生作品',
    student_id         BIGINT       NOT NULL COMMENT '发布学生',
    title              VARCHAR(200) NOT NULL COMMENT '展示标题',
    description        TEXT COMMENT '作品介绍',
    view_count         INT           DEFAULT 0 COMMENT '浏览量',
    like_count         INT           DEFAULT 0 COMMENT '点赞数',
    comment_count      INT           DEFAULT 0 COMMENT '评论数',
    status             TINYINT       DEFAULT 0 COMMENT '0待审核 1已上架 2下架',
    audit_time         DATETIME COMMENT '审核时间',
    audit_user         BIGINT COMMENT '审核管理员ID',
    create_by          BIGINT,
    update_by          BIGINT,
    create_time        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT       DEFAULT 0,
    ext_json           VARCHAR(2000) DEFAULT '{}',
    UNIQUE KEY uk_project (student_project_id),
    INDEX              idx_student_id (student_id),
    INDEX              idx_status (status)
) COMMENT '作品广场公开展示';

DROP TABLE IF EXISTS edu_project_comment;
CREATE TABLE edu_project_comment
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    show_id     BIGINT NOT NULL COMMENT '展示作品ID',
    user_id     BIGINT NOT NULL COMMENT '评论人',
    parent_id   BIGINT   DEFAULT 0 COMMENT '回复评论ID',
    content     TEXT   NOT NULL COMMENT '评论内容',
    status      TINYINT  DEFAULT 1 COMMENT '0屏蔽 1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX       idx_show_id (show_id)
) COMMENT '作品评论';

-- 六、AI 统一接入层模块
DROP TABLE IF EXISTS ai_model_config;
CREATE TABLE ai_model_config
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name  VARCHAR(100) NOT NULL COMMENT '模型名称：千问教育、本地微调模型',
    model_code  VARCHAR(50)  NOT NULL UNIQUE COMMENT '模型唯一编码，后端接口调用标识',
    model_type  TINYINT      NOT NULL COMMENT '1对话大模型 2CV图像模型 3语音模型 4评测打分模型',
    api_url     VARCHAR(255) NOT NULL COMMENT '模型接口地址',
    api_key     VARCHAR(500) COMMENT '密钥',
    temperature DECIMAL(3, 2) DEFAULT 0.7 COMMENT '默认温度参数',
    max_token   INT           DEFAULT 2048 COMMENT '最大输出token',
    quota_daily INT           DEFAULT 10000 COMMENT '每日总调用配额',
    status      TINYINT       DEFAULT 1 COMMENT '0停用 1启用',
    sort        INT           DEFAULT 0,
    create_by   BIGINT,
    update_by   BIGINT,
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    ext_json    VARCHAR(2000) DEFAULT '{}'
) COMMENT '统一AI模型接入配置（AI能力底座核心表）';

DROP TABLE IF EXISTS ai_chat_session;
CREATE TABLE ai_chat_session
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL COMMENT '对话用户',
    scene_type   TINYINT     NOT NULL COMMENT '1学生AI学伴 2教师备课助手 3PBL项目导师',
    target_id    BIGINT COMMENT '关联业务ID：课程ID/项目任务ID',
    session_name VARCHAR(200) COMMENT '会话标题',
    model_code   VARCHAR(50) NOT NULL COMMENT '使用模型编码',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT  DEFAULT 0,
    INDEX        idx_user_id (user_id),
    INDEX        idx_scene_type (scene_type)
) COMMENT 'AI智能助手会话';

DROP TABLE IF EXISTS ai_chat_message;
CREATE TABLE ai_chat_message
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id    BIGINT   NOT NULL COMMENT '会话ID',
    role          TINYINT  NOT NULL COMMENT '0用户提问 1AI回复',
    content       LONGTEXT NOT NULL COMMENT '消息内容',
    reference_ids VARCHAR(1000) COMMENT 'RAG引用知识库文档ID逗号分隔',
    token_cost    INT      DEFAULT 0 COMMENT '消耗token数量',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX         idx_session_id (session_id)
) COMMENT 'AI对话消息记录';

DROP TABLE IF EXISTS ai_invoke_record;
CREATE TABLE ai_invoke_record
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL COMMENT '用户ID',
    model_code   VARCHAR(50) NOT NULL COMMENT '使用的模型编码',
    scene_type   TINYINT     NOT NULL COMMENT '业务场景：1学生AI学伴 2教师备课助手 3PBL项目导师',
    input_token  INT      DEFAULT 0 COMMENT '输入消耗token数量',
    output_token INT      DEFAULT 0 COMMENT '输出消耗token数量',
    total_token  INT      DEFAULT 0 COMMENT '本次调用总token(输入+输出)',
    cost_time    INT      DEFAULT 0 COMMENT '接口耗时ms',
    status       TINYINT     NOT NULL COMMENT '调用状态：0失败 1成功',
    error_msg    TEXT COMMENT '调用失败时存储异常信息',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX        idx_user_id (user_id),
    INDEX        idx_create_time (create_time)
) COMMENT 'AI能力调用日志，用于算力统计、配额管控、调用量对账';

-- 七、RAG 教育知识库模块
DROP TABLE IF EXISTS rag_knowledge_base;
CREATE TABLE rag_knowledge_base
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_name         VARCHAR(200) NOT NULL COMMENT '知识库名称',
    kb_type         TINYINT      NOT NULL COMMENT '1课标文件 2分学段教材 3实验案例 4赛事指南 5校本自定义资源',
    grade           VARCHAR(20) COMMENT '适配学段',
    description     TEXT COMMENT '库说明',
    embedding_model VARCHAR(50)  NOT NULL COMMENT '向量化模型编码',
    status          TINYINT       DEFAULT 1 COMMENT '0停用 1启用',
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    ext_json        VARCHAR(2000) DEFAULT '{}',
    INDEX           idx_kb_type (kb_type)
) COMMENT 'RAG知识库总库';

DROP TABLE IF EXISTS rag_document;
CREATE TABLE rag_document
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id       BIGINT       NOT NULL COMMENT '所属知识库',
    doc_name    VARCHAR(200) NOT NULL COMMENT '文档名称',
    file_url    VARCHAR(255) COMMENT '原始文件地址',
    total_chunk INT           DEFAULT 0 COMMENT '拆分文本块总数',
    status      TINYINT       DEFAULT 0 COMMENT '0解析中 1向量化完成 2解析失败',
    create_by   BIGINT,
    update_by   BIGINT,
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    ext_json    VARCHAR(2000) DEFAULT '{}',
    INDEX       idx_kb_id (kb_id)
) COMMENT '知识库文档';

DROP TABLE IF EXISTS rag_chunk;
CREATE TABLE rag_chunk
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id      BIGINT   NOT NULL COMMENT '归属文档',
    kb_id       BIGINT   NOT NULL COMMENT '归属知识库',
    chunk_text  LONGTEXT NOT NULL COMMENT '文本片段内容',
    chunk_index INT      NOT NULL COMMENT '文档内片段序号',
    vector      TEXT COMMENT '向量数组JSON（简易存储，生产建议向量数据库Milvus）',
    source_page VARCHAR(100) COMMENT '原文页码/段落，溯源使用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX       idx_doc_id (doc_id),
    INDEX       idx_kb_id (kb_id)
) COMMENT '文档拆分向量块，RAG检索数据源';

-- 八、评价 & 学情数据闭环模块
DROP TABLE IF EXISTS edu_project_evaluate;
CREATE TABLE edu_project_evaluate
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_project_id BIGINT NOT NULL COMMENT '学生作品',
    task_id            BIGINT NOT NULL COMMENT '对应项目任务',
    student_id         BIGINT NOT NULL COMMENT '学生ID',
    teacher_id         BIGINT COMMENT '评阅教师ID',
    ai_score           INT      DEFAULT 0 COMMENT 'AI自动评分 0-100',
    teacher_score      INT      DEFAULT 0 COMMENT '教师人工评分 0-100',
    ai_comment         TEXT COMMENT 'AI自动评语',
    teacher_comment    TEXT COMMENT '教师评语',
    evaluate_dim_json  VARCHAR(2000) COMMENT '多维打分JSON：{"计算思维":85,"创新设计":90}',
    evaluate_time      DATETIME COMMENT '评阅时间',
    create_time        DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_eva (student_project_id),
    INDEX              idx_student_id (student_id),
    INDEX              idx_task_id (task_id)
) COMMENT '项目作品评价表';

DROP TABLE IF EXISTS edu_student_portrait;
CREATE TABLE edu_student_portrait
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id          BIGINT   NOT NULL UNIQUE COMMENT '学生ID',
    total_study_hour    INT           DEFAULT 0 COMMENT '累计学习时长',
    total_project_count INT           DEFAULT 0 COMMENT '完成项目总数',
    avg_score           DECIMAL(5, 2) DEFAULT 0 COMMENT '平均项目得分',
    dimension_json      LONGTEXT NOT NULL COMMENT '五大能力维度分数JSON：计算思维、数据处理、模型搭建、创新、协作',
    weak_knowledge      VARCHAR(1000) COMMENT '薄弱知识点ID逗号分隔',
    growth_record       LONGTEXT COMMENT '成长记录快照',
    last_update_time    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_time         DATETIME      DEFAULT CURRENT_TIMESTAMP,
    ext_json            VARCHAR(2000) DEFAULT '{}',
    INDEX               idx_student_id (student_id)
) COMMENT '学生AI素养能力画像';

DROP TABLE IF EXISTS edu_class_analysis;
CREATE TABLE edu_class_analysis
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id            BIGINT NOT NULL COMMENT '班级ID',
    course_id           BIGINT COMMENT '绑定课程，为空代表班级整体',
    avg_score           DECIMAL(5, 2) DEFAULT 0 COMMENT '班级平均分',
    finish_rate         DECIMAL(5, 2) DEFAULT 0 COMMENT '任务完成率',
    dimension_stat      LONGTEXT COMMENT '班级各能力维度分布统计',
    weak_knowledge_stat TEXT COMMENT '班级共性薄弱知识点',
    stat_date           DATE   NOT NULL COMMENT '统计日期',
    create_time         DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_course_date (class_id, course_id, stat_date),
    INDEX               idx_class_id (class_id)
) COMMENT '班级学情分析快照表';

DROP TABLE IF EXISTS edu_growth_file;
CREATE TABLE edu_growth_file
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id  BIGINT       NOT NULL COMMENT '学生ID',
    file_name   VARCHAR(200) NOT NULL COMMENT '档案文件名',
    file_url    VARCHAR(255) NOT NULL COMMENT 'PDF档案地址',
    file_type   TINYINT      NOT NULL COMMENT '1月度档案 2学期档案 3学年完整档案',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX       idx_student_id (student_id)
) COMMENT '学生AI成长电子档案文件';
