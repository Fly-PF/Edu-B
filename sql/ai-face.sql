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


