-- Edu-F 考公专题建表 SQL
-- 依赖现有 sys_user.id；本文件不会自动接入 Spring Boot 初始化配置。
-- 内容字段使用 Markdown + LaTeX；图片仅保存服务器访问地址。

CREATE TABLE IF NOT EXISTS edu_gov_news_category
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL COMMENT '资讯分类名称',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '展示排序，数值越小越靠前',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    create_by   BIGINT COMMENT '创建人ID',
    update_by   BIGINT COMMENT '更新人ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gov_news_category_name (name),
    INDEX idx_gov_news_category_status_sort (status, sort_order)
) COMMENT '考公资讯分类';

CREATE TABLE IF NOT EXISTS edu_gov_news
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id  BIGINT       NOT NULL COMMENT '资讯分类ID',
    title        VARCHAR(200) NOT NULL COMMENT '资讯标题',
    summary      VARCHAR(500) COMMENT '资讯摘要',
    content_md   LONGTEXT     NOT NULL COMMENT 'Markdown + LaTeX正文',
    cover_url    VARCHAR(500) COMMENT '封面图片地址',
    is_top       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1发布 2下架',
    published_at DATETIME COMMENT '发布时间',
    create_by    BIGINT COMMENT '创建人ID',
    update_by    BIGINT COMMENT '更新人ID',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_gov_news_category_status (category_id, status),
    INDEX idx_gov_news_publish (status, is_top, published_at)
) COMMENT '考公资讯公告';

CREATE TABLE IF NOT EXISTS edu_gov_knowledge_node
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject     VARCHAR(30)  NOT NULL COMMENT '行测科目',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父节点ID，根节点为0',
    node_type   VARCHAR(20)  NOT NULL COMMENT 'CHAPTER章节 POINT知识点',
    title       VARCHAR(200) NOT NULL COMMENT '章节或知识点名称',
    content_md  LONGTEXT COMMENT 'Markdown + LaTeX内容',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '同级排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    create_by   BIGINT COMMENT '创建人ID',
    update_by   BIGINT COMMENT '更新人ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_gov_knowledge_tree (subject, parent_id, sort_order),
    INDEX idx_gov_knowledge_status (status)
) COMMENT '考公知识点目录';

CREATE TABLE IF NOT EXISTS edu_gov_knowledge_progress
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL COMMENT '用户ID(sys_user.id)',
    knowledge_id BIGINT      NOT NULL COMMENT '知识点ID',
    status       VARCHAR(20) NOT NULL DEFAULT 'TODO' COMMENT 'TODO未学习 LEARNING学习中 DONE已完成',
    completed_at DATETIME COMMENT '完成时间',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gov_knowledge_progress (user_id, knowledge_id),
    INDEX idx_gov_knowledge_progress_user (user_id, status)
) COMMENT '用户考公知识点进度';

CREATE TABLE IF NOT EXISTS edu_gov_question
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject       VARCHAR(30) NOT NULL COMMENT '行测科目',
    question_type VARCHAR(20) NOT NULL COMMENT 'SINGLE单选 MULTIPLE多选',
    difficulty    TINYINT     NOT NULL DEFAULT 1 COMMENT '难度1-5',
    exam_year     SMALLINT COMMENT '试题年份',
    source_type   VARCHAR(20) NOT NULL DEFAULT 'SIMULATION' COMMENT 'REAL真题 SIMULATION模拟题',
    content_json  JSON        NOT NULL COMMENT '题干、材料、选项、答案、解析和标签',
    status        TINYINT     NOT NULL DEFAULT 0 COMMENT '0草稿 1上架 2下架',
    create_by     BIGINT COMMENT '创建人ID',
    update_by     BIGINT COMMENT '更新人ID',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    INDEX idx_gov_question_filter (subject, question_type, difficulty, exam_year, status),
    INDEX idx_gov_question_source (source_type, exam_year)
) COMMENT '考公行测题目';

CREATE TABLE IF NOT EXISTS edu_gov_question_knowledge
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id  BIGINT   NOT NULL COMMENT '题目ID',
    knowledge_id BIGINT   NOT NULL COMMENT '知识点ID',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_gov_question_knowledge (question_id, knowledge_id),
    INDEX idx_gov_qk_knowledge (knowledge_id)
) COMMENT '考公题目与知识点关联';

CREATE TABLE IF NOT EXISTS edu_gov_practice_record
(
    id                       BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id                  BIGINT      NOT NULL COMMENT '用户ID(sys_user.id)',
    practice_mode            VARCHAR(20) NOT NULL COMMENT 'SPECIAL专项 DAILY每日一练 WRONG错题重做 MOCK随机模拟考试',
    subject                  VARCHAR(30) COMMENT '行测科目，综合练习时为空',
    total_count              INT         NOT NULL DEFAULT 0 COMMENT '题目总数',
    correct_count            INT         NOT NULL DEFAULT 0 COMMENT '答对数',
    duration_limit_seconds   INT COMMENT '限时秒数，仅模拟考试使用',
    score                    DECIMAL(8,2) COMMENT '练习或模拟考试得分',
    status                   VARCHAR(20) NOT NULL DEFAULT 'DOING' COMMENT 'DOING进行中 FINISHED已完成',
    started_at               DATETIME COMMENT '开始时间',
    finished_at              DATETIME COMMENT '完成时间',
    create_time              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                  TINYINT     NOT NULL DEFAULT 0,
    INDEX idx_gov_practice_user_time (user_id, create_time),
    INDEX idx_gov_practice_mode (practice_mode, status)
) COMMENT '考公普通练习与随机模拟考试记录';

CREATE TABLE IF NOT EXISTS edu_gov_practice_answer
(
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    practice_id          BIGINT   NOT NULL COMMENT '练习记录ID',
    question_id          BIGINT   NOT NULL COMMENT '题目ID',
    question_order       INT      NOT NULL COMMENT '本次练习中的题目顺序，从1开始',
    selected_answer_json JSON COMMENT '用户选择，例如 ["A","C"]',
    is_correct            TINYINT COMMENT '0错误 1正确',
    duration_seconds     INT COMMENT '本题用时秒数',
    answered_at          DATETIME COMMENT '作答时间',
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted              TINYINT  NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gov_practice_answer_question (practice_id, question_id),
    UNIQUE KEY uk_gov_practice_answer_order (practice_id, question_order),
    INDEX idx_gov_practice_answer_question (question_id)
) COMMENT '考公练习答题明细';

CREATE TABLE IF NOT EXISTS edu_gov_wrong_question
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT   NOT NULL COMMENT '用户ID(sys_user.id)',
    question_id    BIGINT   NOT NULL COMMENT '题目ID',
    wrong_count    INT      NOT NULL DEFAULT 1 COMMENT '做错次数',
    first_wrong_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_wrong_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status         TINYINT  NOT NULL DEFAULT 1 COMMENT '1待复习 0已掌握',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT  NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gov_wrong_question (user_id, question_id),
    INDEX idx_gov_wrong_user_status (user_id, status, last_wrong_at)
) COMMENT '考公错题本';

CREATE TABLE IF NOT EXISTS edu_gov_user_goal
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '用户ID(sys_user.id)，每个用户一条当前目标',
    exam_type   VARCHAR(30)  COMMENT '国考、省考等',
    exam_name   VARCHAR(100) NOT NULL COMMENT '目标考试名称',
    exam_date   DATE         NOT NULL COMMENT '考试日期',
    note        VARCHAR(500) COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gov_user_goal_user (user_id)
) COMMENT '用户当前公考目标';

CREATE TABLE IF NOT EXISTS edu_gov_plan_task
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL COMMENT '用户ID(sys_user.id)',
    task_date    DATE         NOT NULL COMMENT '任务日期',
    title        VARCHAR(200) NOT NULL COMMENT '便签任务内容',
    task_type    VARCHAR(30) COMMENT 'QUESTION题目 READING阅读 OTHER其他',
    target_value INT COMMENT '可选目标数量，仅作展示，不自动核验',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0未完成 1已完成',
    completed_at DATETIME COMMENT '完成时间',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_gov_plan_task_user_date (user_id, task_date),
    INDEX idx_gov_plan_task_status (user_id, status)
) COMMENT '考公学习便签任务';

CREATE TABLE IF NOT EXISTS edu_gov_material_category
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '资料分类名称',
    sort_order  INT         NOT NULL DEFAULT 0 COMMENT '展示排序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    create_by   BIGINT COMMENT '创建人ID',
    update_by   BIGINT COMMENT '更新人ID',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gov_material_category_name (name),
    INDEX idx_gov_material_category_status_sort (status, sort_order)
) COMMENT '考公资料分类';

CREATE TABLE IF NOT EXISTS edu_gov_material
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id  BIGINT       NOT NULL COMMENT '资料分类ID',
    title        VARCHAR(200) NOT NULL COMMENT '资料名称',
    description  VARCHAR(1000) COMMENT '资料说明',
    links_json   JSON         NOT NULL COMMENT '网盘链接数组，含platform、url、accessCode',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1发布 2下架',
    sort_order   INT          NOT NULL DEFAULT 0 COMMENT '展示排序',
    create_by    BIGINT COMMENT '创建人ID',
    update_by    BIGINT COMMENT '更新人ID',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_gov_material_category_status (category_id, status, sort_order)
) COMMENT '考公网盘资料';
