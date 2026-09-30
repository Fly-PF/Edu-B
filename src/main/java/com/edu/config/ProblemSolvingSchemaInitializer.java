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
import java.util.Locale;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProblemSolvingSchemaInitializer implements ApplicationRunner {
    private static final String VERSION_TABLE_NAME = "edu_problem_version";
    private static final String CATEGORY_TABLE_NAME = "edu_problem_category";
    private static final String PROBLEM_TABLE_NAME = "edu_problem";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            if (!isMysql(connection)) return;
            migrateVersionTable(connection);
            migrateCategoryTable(connection);
            migrateProblemTable(connection);
        } catch (SQLException ex) {
            log.warn("Problem-solving schema compatibility check failed: {}", ex.getMessage());
        }
    }

    private boolean isMysql(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        return metaData.getDatabaseProductName() != null
                && metaData.getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql");
    }

    private void migrateVersionTable(Connection connection) throws SQLException {
        if (!tableExists(connection, VERSION_TABLE_NAME)) return;
        addColumnIfMissing(connection, VERSION_TABLE_NAME, "standard_answer", "ALTER TABLE edu_problem_version ADD COLUMN standard_answer TEXT NULL");
        addColumnIfMissing(connection, VERSION_TABLE_NAME, "answer_analysis", "ALTER TABLE edu_problem_version ADD COLUMN answer_analysis TEXT NULL");
        execute(connection, "UPDATE edu_problem_version SET standard_answer = JSON_UNQUOTE(JSON_EXTRACT(definition_json, '$.facts.answer')) WHERE standard_answer IS NULL");
        execute(connection, "UPDATE edu_problem_version SET answer_analysis = JSON_UNQUOTE(JSON_EXTRACT(definition_json, '$.cards[4].explanations.basic')) WHERE answer_analysis IS NULL");
    }

    private void migrateCategoryTable(Connection connection) throws SQLException {
        if (!tableExists(connection, CATEGORY_TABLE_NAME)) return;
        execute(connection, "UPDATE edu_problem_category c JOIN edu_problem_subject s ON s.id = c.subject_id SET c.name = '二次函数', c.description = '二次函数图像、性质与最值' WHERE s.code = 'MATH' AND c.name = '抛物线动点'");
        execute(connection, "UPDATE edu_problem_category c JOIN edu_problem_subject s ON s.id = c.subject_id SET c.name = '力学', c.description = '受力、运动、能量与动量' WHERE s.code = 'PHYSICS' AND c.name = '单弹簧'");
        dropIndexIfExists(connection, "uk_problem_category_subject_code");
        dropColumnIfExists(connection, "code");
    }

    private void migrateProblemTable(Connection connection) throws SQLException {
        if (!tableExists(connection, PROBLEM_TABLE_NAME)) return;
        addColumnIfMissing(connection, PROBLEM_TABLE_NAME, "create_by", "ALTER TABLE edu_problem ADD COLUMN create_by BIGINT NULL");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        return exists(connection, "SELECT COUNT(1) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?", tableName);
    }

    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String sql) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, sql);
        }
    }

    private void dropColumnIfExists(Connection connection, String columnName) throws SQLException {
        if (columnExists(connection, CATEGORY_TABLE_NAME, columnName)) {
            execute(connection, "ALTER TABLE edu_problem_category DROP COLUMN code");
        }
    }

    private void dropIndexIfExists(Connection connection, String indexName) throws SQLException {
        if (exists(connection, "SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?", CATEGORY_TABLE_NAME, indexName)) {
            execute(connection, "ALTER TABLE edu_problem_category DROP INDEX uk_problem_category_subject_code");
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        return exists(connection, "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?", tableName, columnName);
    }

    private boolean exists(Connection connection, String sql, String... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setString(index + 1, values[index]);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
