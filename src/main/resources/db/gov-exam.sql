-- Edu-F 考公专题建表 SQL
-- 依赖现有 sys_user.id；由 Spring Boot 启动时执行初始化。
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

CREATE TABLE IF NOT EXISTS edu_gov_knowledge_compare
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    knowledge_id BIGINT       NOT NULL COMMENT '知识点ID',
    title        VARCHAR(200) NOT NULL COMMENT '对比标题',
    content_md   LONGTEXT     COMMENT 'Markdown对比内容',
    sort_order   INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    create_by    BIGINT COMMENT '创建人ID',
    update_by    BIGINT COMMENT '更新人ID',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_gov_knowledge_compare_knowledge (knowledge_id, status, sort_order)
) COMMENT '考公知识点易混辨析';

CREATE TABLE IF NOT EXISTS edu_gov_knowledge_favorite
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL COMMENT '用户ID(sys_user.id)',
    knowledge_id BIGINT      NOT NULL COMMENT '知识点ID',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gov_knowledge_favorite (user_id, knowledge_id)
) COMMENT '考公知识点收藏';

CREATE TABLE IF NOT EXISTS edu_gov_knowledge_note
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL COMMENT '用户ID(sys_user.id)',
    knowledge_id BIGINT      NOT NULL COMMENT '知识点ID',
    note_content LONGTEXT    COMMENT '笔记内容',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gov_knowledge_note (user_id, knowledge_id)
) COMMENT '考公知识点笔记';

CREATE TABLE IF NOT EXISTS edu_gov_knowledge_annotation
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL COMMENT '用户ID(sys_user.id)',
    knowledge_id  BIGINT        NOT NULL COMMENT '知识点ID',
    section_key   VARCHAR(100)  NOT NULL COMMENT '正文段落标识',
    section_title VARCHAR(200)  NOT NULL COMMENT '正文段落标题',
    start_offset  INT           NOT NULL COMMENT '段落内起始位置',
    end_offset    INT           NOT NULL COMMENT '段落内结束位置',
    selected_text VARCHAR(2000) NOT NULL COMMENT '选中的正文内容',
    note_content  VARCHAR(5000) NOT NULL COMMENT '标注说明',
    color         VARCHAR(20)   NOT NULL DEFAULT 'lavender' COMMENT '标注颜色 lavender/mint/peach/teal',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    INDEX idx_gov_knowledge_annotation_user (user_id, knowledge_id, deleted),
    INDEX idx_gov_knowledge_annotation_node (knowledge_id, section_key, deleted)
) COMMENT '考公知识点正文标注';

-- 初始化示例数据：按行测六科各放 1 个章节 + 2 个知识点
-- 这部分用于首屏演示和联调，后续可继续扩充。

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '政治理论', 0, 'CHAPTER', '政治理论导学',
    '### 学习建议\n- 先掌握马克思主义基本原理\n- 再理解党的创新理论\n- 最后结合时政热点回顾\n\n这一章主要作为政治理论的学习总入口。',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '政治理论' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '政治理论导学' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '政治理论',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '政治理论' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '政治理论导学' AND deleted = 0 LIMIT 1),
    'POINT', '马克思主义基本原理',
    '### 核心内容\n- 物质决定意识\n- 实践是认识的来源与检验标准\n- 联系、发展、矛盾是辩证法的基本视角',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '政治理论' AND node_type = 'POINT' AND title = '马克思主义基本原理' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '政治理论',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '政治理论' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '政治理论导学' AND deleted = 0 LIMIT 1),
    'POINT', '党的创新理论',
    '### 核心内容\n- 习近平新时代中国特色社会主义思想\n- 全面从严治党\n- 中国式现代化与高质量发展',
    2, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '政治理论' AND node_type = 'POINT' AND title = '党的创新理论' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '常识判断', 0, 'CHAPTER', '常识判断导学',
    '### 学习建议\n- 重点关注法律、科技、人文和地理常识\n- 平时多做积累题\n- 先广后精，避免死记硬背',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '常识判断' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '常识判断导学' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '常识判断',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '常识判断' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '常识判断导学' AND deleted = 0 LIMIT 1),
    'POINT', '法律常识',
    '### 核心内容\n- 宪法基础\n- 民法与行政法常见考点\n- 权利义务和程序意识',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '常识判断' AND node_type = 'POINT' AND title = '法律常识' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '常识判断',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '常识判断' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '常识判断导学' AND deleted = 0 LIMIT 1),
    'POINT', '科技人文与地理',
    '### 核心内容\n- 生活科技与信息技术常识\n- 中国历史文化与传统节日\n- 地理区位、气候与资源分布',
    2, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '常识判断' AND node_type = 'POINT' AND title = '科技人文与地理' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '语言理解与表达', 0, 'CHAPTER', '语言理解导学',
    '### 学习建议\n- 先抓主旨，再看细节\n- 练好语感和逻辑连接\n- 注意同义替换和文段结构',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '语言理解与表达' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '语言理解导学' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '语言理解与表达',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '语言理解与表达' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '语言理解导学' AND deleted = 0 LIMIT 1),
    'POINT', '主旨概括',
    '### 核心内容\n- 找中心句\n- 看反复出现的关键词\n- 关注转折、总结、因果信号',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '语言理解与表达' AND node_type = 'POINT' AND title = '主旨概括' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '语言理解与表达',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '语言理解与表达' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '语言理解导学' AND deleted = 0 LIMIT 1),
    'POINT', '逻辑填空',
    '### 核心内容\n- 通过上下文判断语义\n- 关注搭配、语义轻重和褒贬色彩\n- 多看文段语境，少靠单词猜测',
    2, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '语言理解与表达' AND node_type = 'POINT' AND title = '逻辑填空' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '数量关系', 0, 'CHAPTER', '数量关系导学',
    '### 学习建议\n- 从基础公式和常见题型入手\n- 先掌握解题模型，再训练速度\n- 别硬算，先判断能否用代入和排除',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '数量关系' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '数量关系导学' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '数量关系',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '数量关系' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '数量关系导学' AND deleted = 0 LIMIT 1),
    'POINT', '工程与行程问题',
    '### 核心内容\n- 速度、时间、路程三要素\n- 工程题关注效率和总量\n- 会列式比会口算更重要',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '数量关系' AND node_type = 'POINT' AND title = '工程与行程问题' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '数量关系',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '数量关系' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '数量关系导学' AND deleted = 0 LIMIT 1),
    'POINT', '方程与比例',
    '### 核心内容\n- 设未知量后建立等式\n- 比例关系常用于增长和分配\n- 适合配合代入法一起使用',
    2, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '数量关系' AND node_type = 'POINT' AND title = '方程与比例' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '判断推理', 0, 'CHAPTER', '判断推理导学',
    '### 学习建议\n- 图形题重规则，定义题重条件\n- 先找共性，再找差异\n- 多练归纳和排除',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '判断推理' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '判断推理导学' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '判断推理',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '判断推理' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '判断推理导学' AND deleted = 0 LIMIT 1),
    'POINT', '图形推理',
    '### 核心内容\n- 对称、旋转、数量、位置变化\n- 观察元素的增减和移动\n- 规律通常先简单后复杂',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '判断推理' AND node_type = 'POINT' AND title = '图形推理' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '判断推理',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '判断推理' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '判断推理导学' AND deleted = 0 LIMIT 1),
    'POINT', '类比与定义判断',
    '### 核心内容\n- 类比看关系\n- 定义判断看关键词和限定条件\n- 先审题，再比对选项',
    2, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '判断推理' AND node_type = 'POINT' AND title = '类比与定义判断' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '资料分析', 0, 'CHAPTER', '资料分析导学',
    '### 学习建议\n- 先练读图表，再练速算\n- 常见公式要熟\n- 重点在提取数据与判断趋势',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '资料分析' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '资料分析导学' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '资料分析',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '资料分析' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '资料分析导学' AND deleted = 0 LIMIT 1),
    'POINT', '增长率与比重',
    '### 核心内容\n- 增长率反映变化速度\n- 比重反映结构占比\n- 读数后先判断同比和环比',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '资料分析' AND node_type = 'POINT' AND title = '增长率与比重' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_node (
    subject, parent_id, node_type, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    '资料分析',
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '资料分析' AND parent_id = 0 AND node_type = 'CHAPTER' AND title = '资料分析导学' AND deleted = 0 LIMIT 1),
    'POINT', '增长量与同比环比',
    '### 核心内容\n- 增长量 = 本期量 - 上期量\n- 同比看去年同期，环比看上期\n- 结合增长率一起判断趋势\n\n### 易错提醒\n- 看清时间跨度和基期是否一致',
    4, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_node
    WHERE subject = '资料分析' AND node_type = 'POINT' AND title = '增长量与同比环比' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_compare (
    knowledge_id, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '语言理解与表达' AND node_type = 'POINT' AND title = '主旨概括' AND deleted = 0 LIMIT 1),
    '主旨概括 vs 细节理解',
    '| 维度 | 主旨概括 | 细节理解 |\n|---|---|---|\n| 关注点 | 全文中心 | 局部信息 |\n| 选项特征 | 概括性强 | 具体细节多 |\n| 做题思路 | 先看中心句与转折句 | 锁定原文对应句 |\n\n### 提醒\n主旨题不能选“局部正确但范围过窄”的选项。',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_compare WHERE title = '主旨概括 vs 细节理解' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_compare (
    knowledge_id, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '资料分析' AND node_type = 'POINT' AND title = '增长率与比重' AND deleted = 0 LIMIT 1),
    '同比 vs 环比',
    '| 维度 | 同比 | 环比 |\n|---|---|---|\n| 对比对象 | 去年同期 | 上期 |\n| 作用 | 看年度变化 | 看短期变化 |\n| 常见题型 | 增长率、增量 | 趋势判断 |\n\n### 提醒\n题目问“较上期”通常是环比，问“较去年同期”通常是同比。',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_compare WHERE title = '同比 vs 环比' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_compare (
    knowledge_id, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '判断推理' AND node_type = 'POINT' AND title = '类比与定义判断' AND deleted = 0 LIMIT 1),
    '充分条件 vs 必要条件',
    '| 关系 | 记法 | 说明 |\n|---|---|---|\n| 充分条件 | A → B | 有A一定有B |\n| 必要条件 | B → A | 没有B就没有A |\n| 充分必要 | A ⇔ B | 两边都成立 |\n\n### 提醒\n先判断箭头方向，再看“只有、除非、前提、基础”等关键词。',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_compare WHERE title = '充分条件 vs 必要条件' AND deleted = 0
);

INSERT INTO edu_gov_knowledge_compare (
    knowledge_id, title, content_md, sort_order, status, create_by, update_by, create_time, update_time, deleted
)
SELECT
    (SELECT id FROM edu_gov_knowledge_node WHERE subject = '数量关系' AND node_type = 'POINT' AND title = '方程与比例' AND deleted = 0 LIMIT 1),
    '方程法 vs 代入法',
    '| 方法 | 适用场景 | 特点 |\n|---|---|---|\n| 方程法 | 关系明确、未知量好设 | 稳，但步骤稍多 |\n| 代入法 | 选项少、答案范围小 | 快，但要会排除 |\n\n### 提醒\n先判断题目是否适合直接设未知数，不要一上来就复杂建模。',
    1, 1, NULL, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM edu_gov_knowledge_compare WHERE title = '方程法 vs 代入法' AND deleted = 0
);

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
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted              TINYINT  NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gov_practice_answer_question (practice_id, question_id),
    UNIQUE KEY uk_gov_practice_answer_order (practice_id, question_order),
    INDEX idx_gov_practice_answer_question (question_id)
) COMMENT '考公练习答题明细';

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
