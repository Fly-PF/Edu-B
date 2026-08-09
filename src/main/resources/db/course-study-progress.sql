CREATE TABLE IF NOT EXISTS edu_resource_block_project
(
    resource_id BIGINT PRIMARY KEY,
    project_id  BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resource_block_project_project (project_id)
) COMMENT 'Course resource to public Blockly project relation';

CREATE TABLE IF NOT EXISTS edu_resource_study_record
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id      BIGINT   NOT NULL,
    assignment_id   BIGINT   NOT NULL DEFAULT 0,
    course_id       BIGINT   NOT NULL,
    chapter_id      BIGINT   NOT NULL,
    resource_id     BIGINT   NOT NULL,
    progress        INT      NOT NULL DEFAULT 0,
    study_duration  INT      NOT NULL DEFAULT 0,
    finish_status   TINYINT  NOT NULL DEFAULT 0,
    last_study_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_resource_assignment_student (student_id, assignment_id, resource_id),
    INDEX idx_resource_study_course (course_id, assignment_id),
    INDEX idx_resource_study_student (student_id, assignment_id)
) COMMENT 'Student resource progress scoped to a course assignment';
