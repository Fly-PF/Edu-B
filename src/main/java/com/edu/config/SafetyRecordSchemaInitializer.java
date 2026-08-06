package com.edu.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SafetyRecordSchemaInitializer implements ApplicationRunner {
    private static final String TABLE_NAME = "edu_safety_record";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            if (!isMysql(connection)) {
                return;
            }
            if (!tableExists(connection)) {
                log.info("Safety record table does not exist yet, skip schema compatibility check.");
                return;
            }
            ensureReviewColumns(connection);
            backfillReviewStatus(connection);
        } catch (SQLException ex) {
            log.warn("Safety record schema compatibility check failed: {}", ex.getMessage());
        }
    }

    private boolean isMysql(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String productName = metaData.getDatabaseProductName();
        return productName != null && productName.toLowerCase(Locale.ROOT).contains("mysql");
    }

    private boolean tableExists(Connection connection) throws SQLException {
        String sql = """
                SELECT COUNT(1)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TABLE_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private void ensureReviewColumns(Connection connection) throws SQLException {
        Map<String, String> reviewColumns = new LinkedHashMap<>();
        reviewColumns.put("review_status", "ALTER TABLE edu_safety_record ADD COLUMN review_status VARCHAR(20) DEFAULT 'NOT_REQUIRED' COMMENT 'review status'");
        reviewColumns.put("review_by", "ALTER TABLE edu_safety_record ADD COLUMN review_by BIGINT COMMENT 'review user id'");
        reviewColumns.put("review_by_name", "ALTER TABLE edu_safety_record ADD COLUMN review_by_name VARCHAR(50) COMMENT 'review user name'");
        reviewColumns.put("review_time", "ALTER TABLE edu_safety_record ADD COLUMN review_time DATETIME COMMENT 'review time'");
        reviewColumns.put("review_comment", "ALTER TABLE edu_safety_record ADD COLUMN review_comment TEXT COMMENT 'review comment'");

        for (Map.Entry<String, String> entry : reviewColumns.entrySet()) {
            if (!columnExists(connection, entry.getKey())) {
                execute(connection, entry.getValue());
                log.info("Added missing safety record column: {}", entry.getKey());
            }
        }
    }

    private boolean columnExists(Connection connection, String columnName) throws SQLException {
        String sql = """
                SELECT COUNT(1)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TABLE_NAME);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private void backfillReviewStatus(Connection connection) throws SQLException {
        String pendingSql = """
                UPDATE edu_safety_record
                SET review_status = 'PENDING'
                WHERE manual_review_required = 1
                  AND (review_status IS NULL OR review_status = '' OR review_status = 'NOT_REQUIRED')
                """;
        execute(connection, pendingSql);

        String notRequiredSql = """
                UPDATE edu_safety_record
                SET review_status = 'NOT_REQUIRED'
                WHERE (manual_review_required IS NULL OR manual_review_required = 0)
                  AND (review_status IS NULL OR review_status = '')
                """;
        execute(connection, notRequiredSql);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
