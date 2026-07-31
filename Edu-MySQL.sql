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
    series_name    VARCHAR(100) COMMENT '课程系列名称',
    series_order   INT           DEFAULT 0 COMMENT '系列内展示顺序',
    like_count     INT           DEFAULT 0 COMMENT '点赞数',
    publish_time   DATETIME COMMENT '首次公开发布时间',
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

DROP TABLE IF EXISTS edu_course_category;
CREATE TABLE edu_course_category
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(100)  NOT NULL COMMENT '分类名称',
    sort_order INT           NOT NULL DEFAULT 0 COMMENT '展示排序，数值越小越靠前',
    tags_json  VARCHAR(2000) NOT NULL COMMENT '用于筛选课程的标签 JSON 数组',
    match_all  TINYINT       NOT NULL DEFAULT 0 COMMENT '0匹配任意标签 1必须匹配全部标签',
    create_by  BIGINT,
    update_by  BIGINT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT       DEFAULT 0,
    INDEX idx_category_sort (sort_order)
) COMMENT '平台课程展示分类，不改变课程自身属性';

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

-- 五、AI 展馆与智能创作模块
DROP TABLE IF EXISTS ai_practice_record;
CREATE TABLE ai_practice_record
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id       BIGINT       NOT NULL COMMENT '案例ID',
    user_id       BIGINT       NOT NULL COMMENT '学生ID',
    user_name     VARCHAR(50)  NOT NULL COMMENT '学生姓名',
    practice_type VARCHAR(30)  NOT NULL COMMENT '实践类型',
    input_text    MEDIUMTEXT COMMENT '学生输入内容',
    file_url      VARCHAR(255) COMMENT '附件地址',
    file_name     VARCHAR(255) COMMENT '附件名称',
    answer_text   MEDIUMTEXT COMMENT '作品说明或答案',
    note          MEDIUMTEXT COMMENT '反思记录',
    ai_result_json MEDIUMTEXT COMMENT 'AI 结果 JSON',
    score         INT          DEFAULT 0 COMMENT '评分',
    status        TINYINT      DEFAULT 1 COMMENT '0草稿 1已提交',
    create_by     BIGINT,
    update_by     BIGINT,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    ext_json      VARCHAR(2000) DEFAULT '{}',
    INDEX idx_case_id (case_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) COMMENT 'AI 展馆实践记录';

DROP TABLE IF EXISTS ai_project_case;
CREATE TABLE ai_project_case
(
    id                     BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_code           VARCHAR(100)  NOT NULL UNIQUE COMMENT '案例编码',
    project_name           VARCHAR(200)  NOT NULL COMMENT '项目名称',
    case_summary           VARCHAR(500)  NOT NULL COMMENT '案例摘要',
    grade_band             VARCHAR(50)   NOT NULL COMMENT '适合年级',
    subject_direction      VARCHAR(100)  NOT NULL COMMENT '学科方向',
    project_background     MEDIUMTEXT    NOT NULL COMMENT '项目背景',
    learning_goals_json    MEDIUMTEXT    NOT NULL COMMENT '学习目标 JSON',
    ai_capability          VARCHAR(50)   NOT NULL COMMENT 'AI 能力类型',
    practice_type          VARCHAR(30)   NOT NULL COMMENT '实践类型',
    task_steps_json        MEDIUMTEXT    NOT NULL COMMENT '任务步骤 JSON',
    required_tools_json    MEDIUMTEXT    NOT NULL COMMENT '所需工具 JSON',
    example_code           MEDIUMTEXT    NOT NULL COMMENT '示例代码',
    submission_requirements MEDIUMTEXT   NOT NULL COMMENT '提交作品要求',
    evaluation_rubric_json MEDIUMTEXT    NOT NULL COMMENT '评价 Rubric JSON',
    cover                  VARCHAR(255)  COMMENT '封面地址',
    tags_json              MEDIUMTEXT    COMMENT '标签 JSON',
    challenge_level        TINYINT       DEFAULT 1 COMMENT '挑战等级',
    sort                   INT           DEFAULT 0 COMMENT '排序',
    status                 TINYINT       DEFAULT 1 COMMENT '0停用 1启用',
    create_by              BIGINT,
    update_by              BIGINT,
    create_time            DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       DEFAULT 0,
    ext_json               VARCHAR(2000)  DEFAULT '{}',
    INDEX idx_grade_band (grade_band),
    INDEX idx_subject_direction (subject_direction),
    INDEX idx_practice_type (practice_type),
    INDEX idx_status_sort (status, sort)
) COMMENT 'AI 项目式学习案例';

INSERT INTO ai_project_case
(project_code, project_name, case_summary, grade_band, subject_direction, project_background, learning_goals_json,
 ai_capability, practice_type, task_steps_json, required_tools_json, example_code, submission_requirements,
 evaluation_rubric_json, cover, tags_json, challenge_level, sort, status, create_by, update_by, ext_json)
VALUES
(
    'waste_sorting_assistant',
    '校园垃圾分类助手',
    '让学生用 AI 识别垃圾图片并给出分类与投放建议。',
    '小学高年级',
    '综合实践',
    '校园里每天都会产生各种垃圾。学生可以围绕“如何让垃圾分类更准确”这个真实问题，设计一个能识别垃圾图片并提示分类结果的小应用。',
    '["认识可回收物、厨余垃圾、有害垃圾和其他垃圾","练习调用视觉模型接口","理解分类结果如何转化为生活建议"]',
    'vision',
    'vision',
    '["观察校园常见垃圾并整理样本","调用现成视觉模型上传图片","把模型返回结果转成分类标签和投放提示","整理成可展示的小应用页面"]',
    '["视觉模型 API","图片上传控件","前端展示页面"]',
    'import requests\n\nprompt = "识别图片中的垃圾类别，并返回可回收物、厨余垃圾、有害垃圾或其他垃圾。"\nresp = requests.post(\n    "https://api.example.com/v1/vision",\n    json={\n        "model": "demo-vision",\n        "prompt": prompt,\n        "image_url": image_url\n    }\n)\nprint(resp.json())',
    '提交 1 张垃圾图片、1 段分类说明和 1 个结果展示页截图。',
    '[{"criterion":"分类是否正确","excellent":"类别判断准确且理由清楚","good":"类别基本正确，理由较完整","pass":"能给出分类结果","needsImprovement":"分类结果不稳定或缺少理由"},{"criterion":"交互是否完整","excellent":"有上传、识别和展示闭环","good":"主要交互完整","pass":"能运行核心流程","needsImprovement":"流程不完整"},{"criterion":"表达是否清晰","excellent":"结果图文并茂，适合学生理解","good":"表达基本清楚","pass":"能够看懂","needsImprovement":"结果难以理解"}]',
    NULL,
    '["垃圾分类","视觉识别","校园生活","环保"]',
    1,
    1,
    1,
    1,
    1,
    '{}'
),
(
    'wrong_answer_helper',
    '错题讲解小助手',
    '学生输入一道数学题，AI 自动给出解题思路、知识点和相似练习。',
    '初中',
    '数学',
    '很多学生会做题，但不知道自己错在哪里。这个项目让学生把“老师讲解”拆成一个可调用的大模型能力，形成可追问、可复习的小助手。',
    '["学会向大模型提交题目并读取结构化回答","理解解题过程和知识点提炼方式","练习把大模型输出整理成学习卡片"]',
    'llm',
    'text',
    '["输入一道错题或拍照转写后的题目","调用大语言模型生成解题思路","提炼知识点、易错点和相似练习","展示成答题卡和巩固练习页"]',
    '["大语言模型 API","文本输入框","结果卡片"]',
    'import requests\n\nprompt = f"请分步骤讲解这道数学题：{question}"\nresp = requests.post(\n    "https://api.example.com/v1/chat/completions",\n    json={\n        "model": "demo-llm",\n        "messages": [{"role": "user", "content": prompt}]\n    }\n)\nprint(resp.json())',
    '提交题目文本、讲解思路和 2 道相似练习题。',
    '[{"criterion":"讲解质量","excellent":"步骤清晰、逻辑完整","good":"能说明主要思路","pass":"有基本讲解","needsImprovement":"讲解过于简略"},{"criterion":"知识点提炼","excellent":"能准确提炼并举例","good":"知识点基本准确","pass":"能说出核心知识点","needsImprovement":"知识点不清楚"},{"criterion":"练习设计","excellent":"练习有梯度且贴近题目","good":"练习基本相关","pass":"有相似练习","needsImprovement":"练习与题目关联弱"}]',
    NULL,
    '["数学","讲解","大模型","错题"]',
    2,
    2,
    1,
    1,
    1,
    '{}'
),
(
    'emotion_diary_analysis',
    '情绪日记分析',
    '学生写一段日记，AI 判断情绪倾向并给出积极建议。',
    '初中',
    '心理健康',
    '成长过程中，学生需要学会观察和表达情绪。这个项目让学生用 AI 辅助分析日记中的情绪词和情绪趋势，建立温和的自我觉察。',
    '["理解情绪识别和文本分析的基础方法","学会把 AI 结果转化为积极建议","培养自我表达和自我调节能力"]',
    'llm',
    'text',
    '["输入一段日记或心情记录","调用大语言模型判断情绪倾向","生成关怀建议和情绪关键词","整理成每日情绪分析页面"]',
    '["大语言模型 API","文本输入框","情绪词云或标签展示"]',
    'import requests\n\nprompt = "请分析下面日记的情绪倾向，并给出 3 条积极建议：\\n" + diary_text\nresp = requests.post(\n    "https://api.example.com/v1/chat/completions",\n    json={\n        "model": "demo-llm",\n        "messages": [{"role": "user", "content": prompt}]\n    }\n)\nprint(resp.json())',
    '提交一段日记、情绪分析结果和一条自我调节建议。',
    '[{"criterion":"情绪判断","excellent":"能准确识别并解释情绪","good":"情绪判断基本合理","pass":"能给出情绪方向","needsImprovement":"判断过于笼统"},{"criterion":"建议质量","excellent":"建议具体且可执行","good":"建议较积极","pass":"有基本建议","needsImprovement":"建议太空泛"},{"criterion":"表达体验","excellent":"界面温和、友好、清晰","good":"展示基本完整","pass":"能看懂结果","needsImprovement":"展示不够友好"}]',
    NULL,
    '["心理健康","日记","文本分析","关怀"]',
    2,
    3,
    1,
    1,
    1,
    '{}'
),
(
    'plant_recognition',
    '校园植物识别',
    '学生拍一张植物图片，AI 识别名称并生成科普介绍。',
    '小学高年级',
    '科学',
    '校园里有许多常见植物。学生可以围绕“我身边这株植物叫什么”展开观察，用 AI 识别图片并输出科普介绍。',
    '["认识图像识别的基本过程","学会把识别结果整理成科普文本","培养校园观察和自然探索意识"]',
    'vision',
    'vision',
    '["拍摄植物图片","调用视觉模型识别植物","生成科普介绍和观察要点","整理成校园植物名片"]',
    '["视觉模型 API","拍照上传控件","科普卡片模板"]',
    'import requests\n\nresp = requests.post(\n    "https://api.example.com/v1/vision",\n    json={\n        "model": "demo-vision",\n        "image_url": image_url,\n        "task": "识别植物并输出科普介绍"\n    }\n)\nprint(resp.json())',
    '提交植物照片、识别结果和一段 100 字以内的科普介绍。',
    '[{"criterion":"识别结果","excellent":"识别准确且信息丰富","good":"识别基本准确","pass":"能给出名称","needsImprovement":"识别缺少依据"},{"criterion":"科普表达","excellent":"语言通俗且有知识点","good":"介绍清楚","pass":"能看懂","needsImprovement":"介绍太简单"},{"criterion":"作品呈现","excellent":"卡片完整、美观、信息结构清晰","good":"呈现较完整","pass":"能展示内容","needsImprovement":"展示较散乱"}]',
    NULL,
    '["科学","植物","视觉识别","科普"]',
    1,
    4,
    1,
    1,
    1,
    '{}'
),
(
    'poetry_learning_assistant',
    '古诗词学习助手',
    '输入一首古诗，AI 解释意思、分析意象并生成背诵练习。',
    '小学高年级',
    '语文',
    '古诗词学习常常卡在“看懂字面意思”和“体会意境”之间。学生可以通过 AI 把原文、注释、意象和练习整合成一个学习小作品。',
    '["理解大模型如何辅助文本解读","学会把诗句拆解为意思、意象和情感","练习生成背诵卡片和填空题"]',
    'llm',
    'text',
    '["输入一首古诗","调用大语言模型解释意思和意象","生成背诵练习和填空题","整理成古诗学习卡"]',
    '["大语言模型 API","文本输入框","练习卡片模板"]',
    'import requests\n\nprompt = "请解释这首古诗的意思、意象和情感，并生成 3 道背诵练习题：\\n" + poem_text\nresp = requests.post(\n    "https://api.example.com/v1/chat/completions",\n    json={\n        "model": "demo-llm",\n        "messages": [{"role": "user", "content": prompt}]\n    }\n)\nprint(resp.json())',
    '提交古诗文本、解释内容和一组练习题。',
    '[{"criterion":"文本解读","excellent":"意思、意象、情感都讲清楚","good":"能解释主要内容","pass":"能做基本翻译","needsImprovement":"理解偏差较大"},{"criterion":"练习设计","excellent":"题型多样且能巩固记忆","good":"练习较合理","pass":"有练习题","needsImprovement":"练习太少或不相关"},{"criterion":"作品完整性","excellent":"学习卡结构清晰，适合课堂展示","good":"内容较完整","pass":"能完成提交","needsImprovement":"作品不完整"}]',
    NULL,
    '["语文","古诗词","文本理解","背诵"]',
    2,
    5,
    1,
    1,
    1,
    '{}'
);

INSERT INTO ai_project_case
(project_code, project_name, case_summary, grade_band, subject_direction, project_background, learning_goals_json,
 ai_capability, practice_type, task_steps_json, required_tools_json, example_code, submission_requirements,
 evaluation_rubric_json, cover, tags_json, challenge_level, sort, status, create_by, update_by, ext_json)
VALUES
(
    'face_recognition',
    '人脸识别体验',
    '学生通过摄像头录入人脸，再调用现成人脸比对 API 判断当前画面是否与录入人脸一致。',
    '初中',
    '信息科技',
    '人脸识别是图像识别在真实生活中的常见应用。本案例让学生在合规、受控的课堂环境中观察摄像头采集、前端实时画面、后端 API 调用和相似度阈值判断的完整流程。',
    '["理解浏览器摄像头采集和视频画面展示的基本过程","体验人脸检测、人脸比对和相似度阈值的应用方式","认识生物特征数据的隐私保护要求"]',
    'vision',
    'vision',
    '["打开摄像头并观察实时画面","录入一张清晰人脸图片","再次拍摄当前画面并调用人脸比对 API","查看相似度、阈值和是否通过的结果"]',
    '["浏览器摄像头 API","Canvas 截图能力","腾讯云人脸识别 CompareFace API"]',
    'const stream = await navigator.mediaDevices.getUserMedia({ video: true });\nvideo.srcObject = stream;\nconst formData = new FormData();\nformData.append("file", capturedFaceFile);\nawait fetch("/api/ai-face/compare", { method: "POST", body: formData });',
    '提交录入截图、比对截图、相似度结果和一段关于人脸数据隐私保护的说明。',
    '[{"criterion":"流程完整性","excellent":"能完整展示摄像头采集、录入和比对结果","good":"主要流程完整","pass":"能完成录入和比对","needsImprovement":"流程不完整"},{"criterion":"结果理解","excellent":"能解释相似度和阈值含义","good":"能基本说明比对结果","pass":"能读懂是否通过","needsImprovement":"不能说明结果依据"},{"criterion":"隐私意识","excellent":"能说明最小化采集和授权使用原则","good":"有基本隐私保护意识","pass":"能注意不随意传播照片","needsImprovement":"缺少隐私保护说明"}]',
    NULL,
    '["人脸识别","视觉识别","摄像头","隐私保护"]',
    2,
    6,
    1,
    1,
    1,
    '{}'
);

-- 六、人脸识别记录模块
DROP TABLE IF EXISTS ai_face_compare_record;
CREATE TABLE ai_face_compare_record
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL COMMENT '用户ID',
    user_name           VARCHAR(50)  NOT NULL COMMENT '用户名称',
    profile_id          BIGINT       NOT NULL COMMENT '录入记录ID',
    profile_image_url   VARCHAR(255) COMMENT '录入图片地址',
    compare_image_url   VARCHAR(255) COMMENT '比对图片地址',
    compare_image_object VARCHAR(255) COMMENT '比对图片存储对象',
    score               DOUBLE       NOT NULL COMMENT '相似度',
    threshold           DOUBLE       NOT NULL COMMENT '阈值',
    matched             TINYINT      NOT NULL DEFAULT 0 COMMENT '是否通过',
    provider            VARCHAR(50)  NOT NULL COMMENT '调用方',
    request_id          VARCHAR(100) COMMENT '请求ID',
    raw_result_json     MEDIUMTEXT COMMENT '原始结果',
    create_by           BIGINT,
    update_by           BIGINT,
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT      DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_profile_id (profile_id),
    INDEX idx_create_time (create_time)
) COMMENT 'AI 人脸比对记录';

-- 七、积木工坊项目模块
DROP TABLE IF EXISTS block_project;
CREATE TABLE block_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    owner_name VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT '',
    workspace_json LONGTEXT NOT NULL,
    stage_json LONGTEXT NOT NULL,
    thumbnail_data LONGTEXT,
    visibility TINYINT NOT NULL DEFAULT 0 COMMENT '0 private, 1 public',
    source_project_id BIGINT,
    remix_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    published_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_block_project_owner (owner_id, update_time),
    INDEX idx_block_project_gallery (visibility, published_time),
    INDEX idx_block_project_source (source_project_id)
) COMMENT 'Blockly workshop projects';


DROP TABLE IF EXISTS rag_chat_session;
CREATE TABLE rag_chat_session
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT  NOT NULL COMMENT '对话用户',
    session_name VARCHAR(200) COMMENT '会话标题',
    kb_ref_count TINYINT NOT NULL COMMENT '会话依据的知识库数量',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT  DEFAULT 0,
    INDEX idx_user_id (user_id)
) COMMENT 'RAG聊天会话';

DROP TABLE IF EXISTS rag_session_kb_ref;
CREATE TABLE rag_session_kb_ref
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id  BIGINT NOT NULL COMMENT '对话会话ID',
    kb_id       BIGINT NOT NULL COMMENT '知识库ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT  DEFAULT 0,
    UNIQUE KEY uk_session_kb (session_id, kb_id),
    INDEX idx_session_id (session_id)
) COMMENT '会话绑定知识库关联表';

DROP TABLE IF EXISTS rag_chat_message;
CREATE TABLE rag_chat_message
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id    BIGINT      NOT NULL COMMENT '会话ID',
    message_id    VARCHAR(64) NOT NULL COMMENT '消息唯一标识ID(ID+role作为唯一标识)',
    role          VARCHAR(20) NOT NULL COMMENT '消息角色（如user/assistant/system）',
    content       LONGTEXT    NOT NULL COMMENT '消息内容',
    metadata      JSON COMMENT '消息元数据（如扩展字段等，JSON格式存储）',
    doc_ref_count TINYINT     NOT NULL COMMENT '消息依据的文档数量',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT  DEFAULT 0,
    INDEX idx_session_id (session_id),
    UNIQUE KEY uk_message_id (message_id)
) COMMENT 'AI对话消息记录';

DROP TABLE IF EXISTS rag_msg_doc_ref;
CREATE TABLE rag_msg_doc_ref
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_id      BIGINT NOT NULL COMMENT '消息主键ID（rag_chat_message.id）',
    doc_id      BIGINT NOT NULL COMMENT '引用文档ID（rag_document.id）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT  DEFAULT 0,
    UNIQUE KEY uk_msg_doc (msg_id, doc_id),
    INDEX idx_msg_id (msg_id)
) COMMENT 'AI消息引用文档关联表';

DROP TABLE IF EXISTS rag_kb_user_collection;
CREATE TABLE rag_kb_user_collection
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    kb_id       BIGINT NOT NULL COMMENT '知识库ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT  DEFAULT 0,
    UNIQUE KEY uk_user_kb (user_id, kb_id)
) COMMENT 'RAG知识库用户收藏表';

DROP TABLE IF EXISTS rag_knowledge_base;
CREATE TABLE rag_knowledge_base
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    kb_name     VARCHAR(200) NOT NULL COMMENT '知识库名称',
    kb_cover    VARCHAR(255) NOT NULL COMMENT '封面图链接URL',
    description TEXT COMMENT '库说明',
    kb_type     TINYINT      NOT NULL COMMENT '1其他 2课程 3教材 4政策',
    is_public   TINYINT  DEFAULT 0 COMMENT '0私有 1平台公开',
    status      TINYINT  DEFAULT 1 COMMENT '0停用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT  DEFAULT 0
) COMMENT 'RAG知识库总库';

DROP TABLE IF EXISTS rag_document;
CREATE TABLE rag_document
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id       BIGINT       NOT NULL COMMENT '所属知识库',
    doc_name    VARCHAR(200) NOT NULL COMMENT '文档名称',
    doc_type    VARCHAR(20)  NOT NULL COMMENT '文档类型（如：.txt/.pdf/.docx等）',
    description TEXT COMMENT '文档说明',
    file_url    VARCHAR(255) NOT NULL COMMENT '原始文件地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext_json    JSON COMMENT '扩展字段，JSON',
    deleted     TINYINT  DEFAULT 0,
    INDEX idx_kb_id (kb_id)
) COMMENT '知识库文档';
