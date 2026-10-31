package com.fpa.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    private static JdbcTemplate jdbc;
    private static TransactionTemplate transactions;

    @BeforeAll
    static void migrateDatabase() {
        DataSource dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        );
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/common", "classpath:db/migration/dev")
            .load()
            .migrate();
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void appliesAllMigrationsAndCreatesNineTablesAndTwoViews() {
        Set<String> tables = Set.of(
            "app_user", "federated_database", "analytics_contribution",
            "dataset_metadata", "dataset_column_schema", "privacy_budget_ledger",
            "federated_result", "federated_run_participant", "audit_log"
        );
        Set<String> actualTables = jdbc.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = current_schema()",
            String.class
        ).stream().filter(tables::contains).collect(Collectors.toSet());
        assertEquals(tables, actualTables);

        Set<String> actualViews = jdbc.queryForList(
            "SELECT viewname FROM pg_views WHERE schemaname = current_schema() AND viewname LIKE 'v\\_%' ESCAPE '\\'",
            String.class
        ).stream().collect(Collectors.toSet());
        assertTrue(actualViews.contains("v_organization_contribution_summary"));
        assertTrue(actualViews.contains("v_federated_analytics_overview"));

        Integer migrationCount = jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertEquals(12, migrationCount);
    }

    @Test
    void viewsHavingAggregateProcedureAndContributionTriggersWorkInPostgres() {
        long databaseId = createDatabase();
        long contributionId = insertContribution(databaseId, 20, "120.00");

        Integer summaryCount = jdbc.queryForObject(
            "SELECT contribution_count FROM v_organization_contribution_summary WHERE database_id = ?",
            Integer.class,
            databaseId
        );
        assertEquals(1, summaryCount);
        Integer globalCount = jdbc.queryForObject(
            "SELECT total_contributions FROM v_federated_analytics_overview",
            Integer.class
        );
        assertTrue(globalCount >= 1);

        Integer havingCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM (" +
                "SELECT fd.id FROM federated_database fd " +
                "JOIN analytics_contribution ac ON ac.database_id = fd.id " +
                "WHERE fd.id = ? GROUP BY fd.id HAVING COUNT(ac.id) >= ?" +
            ") qualifying_organizations",
            Integer.class,
            databaseId,
            1
        );
        assertEquals(1, havingCount);

        String procedureSummary = jdbc.queryForObject(
            "CALL sp_generate_organization_analytics_summary(?, NULL)",
            (resultSet, rowNum) -> resultSet.getString(1),
            databaseId
        );
        assertTrue(procedureSummary.contains("\"databaseId\""));
        assertTrue(procedureSummary.contains("\"contributionCount\": 1"));

        Integer insertAudit = auditCount(contributionId, "CONTRIBUTION_SUBMITTED");
        assertEquals(1, insertAudit);

        jdbc.update("UPDATE analytics_contribution SET record_count = 25, epsilon = 0.7500 WHERE id = ?", contributionId);
        assertEquals(1, auditCount(contributionId, "CONTRIBUTION_UPDATED"));

        jdbc.update("DELETE FROM analytics_contribution WHERE id = ?", contributionId);
        assertEquals(1, auditCount(contributionId, "CONTRIBUTION_DELETED"));
    }

    @Test
    void rollbackRevertsContributionAndItsTriggerAuditRow() {
        long databaseId = createDatabase();
        long[] contributionId = new long[1];
        RuntimeException failure = assertThrows(RuntimeException.class, () -> transactions.execute(status -> {
            contributionId[0] = insertContribution(databaseId, 12, "48.00");
            throw new IllegalStateException("rollback integration test");
        }));
        assertEquals("rollback integration test", failure.getMessage());
        assertEquals(0, jdbc.queryForObject(
            "SELECT COUNT(*) FROM analytics_contribution WHERE database_id = ?",
            Integer.class,
            databaseId
        ));
        assertEquals(0, auditCount(contributionId[0], "CONTRIBUTION_SUBMITTED"));
    }

    private static long createDatabase() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long userId = jdbc.queryForObject(
            "INSERT INTO app_user (full_name, email, password_hash, role) " +
                "VALUES (?, ?, 'test-hash', 'DATA_OWNER') RETURNING id",
            Long.class,
            "Integration Owner",
            "owner-" + suffix + "@integration.test"
        );
        return jdbc.queryForObject(
            "INSERT INTO federated_database (organization_name, db_identifier, owner_user_id) " +
                "VALUES (?, ?, ?) RETURNING id",
            Long.class,
            "Integration Organization " + suffix,
            "integration-" + suffix,
            userId
        );
    }

    private static long insertContribution(long databaseId, int recordCount, String noisySum) {
        return jdbc.queryForObject(
            "INSERT INTO analytics_contribution (database_id, record_count, noisy_sum, epsilon) " +
                "VALUES (?, ?, ?, 0.5000) RETURNING id",
            Long.class,
            databaseId,
            recordCount,
            noisySum
        );
    }

    private static int auditCount(long contributionId, String action) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE entity_id = ? AND action = ?",
            Integer.class,
            contributionId,
            action
        );
    }
}
