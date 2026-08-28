package com.edu.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GovAssessmentSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            if (!isMysql(connection)) {
                return;
            }
            createTables(connection);
        } catch (SQLException ex) {
            log.warn("Gov assessment schema initialization skipped: {}", ex.getMessage());
        }
    }

    private boolean isMysql(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        return metaData.getDatabaseProductName() != null
                && metaData.getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql");
    }

    private void createTables(Connection connection) throws SQLException {
        List<String> statements = List.of(
                """
                CREATE TABLE IF NOT EXISTS edu_gov_question (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    subject VARCHAR(30) NOT NULL,
                    question_type VARCHAR(20) NOT NULL,
                    difficulty TINYINT NOT NULL DEFAULT 1,
                    exam_year SMALLINT NULL,
                    source_type VARCHAR(20) NOT NULL DEFAULT 'SIMULATION',
                    content_json JSON NOT NULL,
                    status TINYINT NOT NULL DEFAULT 0,
                    create_by BIGINT NULL,
                    update_by BIGINT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    deleted TINYINT NOT NULL DEFAULT 0,
                    INDEX idx_gov_question_filter (status, deleted, subject, difficulty)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS edu_gov_practice_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    practice_mode VARCHAR(20) NOT NULL,
                    subject VARCHAR(30) NULL,
                    total_count INT NOT NULL DEFAULT 0,
                    correct_count INT NOT NULL DEFAULT 0,
                    duration_limit_seconds INT NOT NULL DEFAULT 0,
                    score DECIMAL(8,2) NOT NULL DEFAULT 0,
                    status VARCHAR(20) NOT NULL,
                    started_at DATETIME NOT NULL,
                    finished_at DATETIME NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    deleted TINYINT NOT NULL DEFAULT 0,
                    INDEX idx_gov_practice_user (user_id, practice_mode, status, deleted)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS edu_gov_practice_answer (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    practice_id BIGINT NOT NULL,
                    question_id BIGINT NOT NULL,
                    question_order INT NOT NULL,
                    selected_answer_json JSON NOT NULL,
                    is_correct TINYINT NOT NULL DEFAULT 0,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    deleted TINYINT NOT NULL DEFAULT 0,
                    INDEX idx_gov_answer_practice (practice_id, question_order)
                )
                """
        );

        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }
}

