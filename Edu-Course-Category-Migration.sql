USE edu;

-- MySQL 5.7/8.0 compatible: execute each ALTER only when its column is absent.
SET @series_name_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE edu_course ADD COLUMN series_name VARCHAR(100) COMMENT ''课程系列名称'' AFTER total_chapter',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_course' AND COLUMN_NAME = 'series_name'
);
PREPARE series_name_statement FROM @series_name_sql;
EXECUTE series_name_statement;
DEALLOCATE PREPARE series_name_statement;

SET @series_order_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE edu_course ADD COLUMN series_order INT DEFAULT 0 COMMENT ''系列内展示顺序'' AFTER series_name',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_course' AND COLUMN_NAME = 'series_order'
);
PREPARE series_order_statement FROM @series_order_sql;
EXECUTE series_order_statement;
DEALLOCATE PREPARE series_order_statement;

SET @like_count_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE edu_course ADD COLUMN like_count INT DEFAULT 0 COMMENT ''点赞数'' AFTER series_order',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_course' AND COLUMN_NAME = 'like_count'
);
PREPARE like_count_statement FROM @like_count_sql;
EXECUTE like_count_statement;
DEALLOCATE PREPARE like_count_statement;

SET @publish_time_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE edu_course ADD COLUMN publish_time DATETIME COMMENT ''首次公开发布时间'' AFTER like_count',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_course' AND COLUMN_NAME = 'publish_time'
);
PREPARE publish_time_statement FROM @publish_time_sql;
EXECUTE publish_time_statement;
DEALLOCATE PREPARE publish_time_statement;

UPDATE edu_course
SET publish_time = COALESCE(update_time, create_time)
WHERE status = 1 AND publish_time IS NULL;

CREATE TABLE IF NOT EXISTS edu_course_category
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL COMMENT '分类名称',
    sort_order  INT           NOT NULL DEFAULT 0 COMMENT '展示排序，数值越小越靠前',
    tags_json   VARCHAR(2000) NOT NULL COMMENT '用于筛选课程的标签 JSON 数组',
    match_all   TINYINT       NOT NULL DEFAULT 0 COMMENT '0匹配任意标签 1必须匹配全部标签',
    create_by   BIGINT,
    update_by   BIGINT,
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    INDEX idx_course_category_sort (sort_order)
) COMMENT '平台课程展示分类，不改变课程自身属性';
