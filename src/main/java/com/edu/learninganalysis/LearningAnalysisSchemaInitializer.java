package com.edu.learninganalysis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Owns only the learning-growth tables. This keeps a fresh local database
 * from failing with a missing-table error before the SQL demo script is run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningAnalysisSchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createLearningGrowthTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS edu_learning_case (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    class_id BIGINT NOT NULL,
                    course_id BIGINT NOT NULL,
                    chapter_id BIGINT NULL,
                    student_id BIGINT NOT NULL,
                    teacher_id BIGINT NOT NULL,
                    risk_score INT NOT NULL DEFAULT 0,
                    risk_level VARCHAR(20) NOT NULL,
                    behavior_snapshot TEXT NOT NULL,
                    diagnosis VARCHAR(1000) NOT NULL,
                    diagnosis_source VARCHAR(30) NOT NULL,
                    model_name VARCHAR(120) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_learning_case_class (class_id),
                    INDEX idx_learning_case_student (student_id),
                    INDEX idx_learning_case_course (course_id),
                    INDEX idx_learning_case_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI学习诊断案例'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS edu_learning_plan (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    case_id BIGINT NOT NULL,
                    title VARCHAR(200) NOT NULL,
                    learning_goal VARCHAR(1000) NOT NULL,
                    task_steps TEXT NOT NULL,
                    duration_minutes INT NOT NULL,
                    acceptance_criteria VARCHAR(1000) NOT NULL,
                    check_question VARCHAR(1000) NOT NULL,
                    expected_signals TEXT NOT NULL,
                    teacher_decision VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_learning_plan_case (case_id),
                    INDEX idx_learning_plan_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI学习微计划'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS edu_learning_evidence (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    plan_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    reflection VARCHAR(2000) NOT NULL,
                    difficulty VARCHAR(1000) NOT NULL,
                    answer VARCHAR(2000) NOT NULL,
                    ai_assessment VARCHAR(1000) NOT NULL,
                    confidence INT NOT NULL DEFAULT 0,
                    result VARCHAR(30) NOT NULL,
                    assessment_source VARCHAR(30) NOT NULL,
                    teacher_conclusion VARCHAR(1000) NULL,
                    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    reviewed_at DATETIME NULL,
                    INDEX idx_learning_evidence_plan (plan_id),
                    INDEX idx_learning_evidence_student (student_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生学习证据与理解检查'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS edu_learning_ai_trace (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    case_id BIGINT NULL,
                    plan_id BIGINT NULL,
                    student_id BIGINT NOT NULL,
                    operation VARCHAR(30) NOT NULL,
                    model_name VARCHAR(120) NOT NULL,
                    source VARCHAR(30) NOT NULL,
                    context_summary VARCHAR(1000) NOT NULL,
                    elapsed_millis BIGINT NOT NULL DEFAULT 0,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_learning_trace_case (case_id),
                    INDEX idx_learning_trace_plan (plan_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学情AI生成轨迹'
                """);
        jdbcTemplate.execute("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI课程推荐结果'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS edu_learning_wrong_book (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    student_id BIGINT NOT NULL,
                    name VARCHAR(40) NOT NULL,
                    sort_order INT NOT NULL DEFAULT 0,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_wrong_book_student_name (student_id, name),
                    INDEX idx_wrong_book_student (student_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生自定义错题本'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS edu_learning_wrong_book_item (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    book_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    practice_id BIGINT NOT NULL,
                    question_id BIGINT NOT NULL,
                    practice_title VARCHAR(255) NULL,
                    course_name VARCHAR(255) NULL,
                    question_content TEXT NOT NULL,
                    question_score INT NULL,
                    awarded_score INT NULL,
                    reference_answer TEXT NULL,
                    explanation TEXT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_wrong_book_question (book_id, practice_id, question_id),
                    INDEX idx_wrong_book_item_student (student_id),
                    INDEX idx_wrong_book_item_book (book_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本题目快照'
                """);
        log.info("Learning-growth schema is ready");
    }
}
