package org.acme.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ProjectarbeitMigrationTest {

    @Inject
    DataSource dataSource;

    @Test
    void flywayAppliedProjectarbeitMigrations() throws SQLException {
        String sql = "SELECT COUNT(*) FROM \"flyway_schema_history\" "
                + "WHERE \"success\" = TRUE AND \"version\" IN ('1', '2')";

        assertEquals(2, queryInt(sql));
    }

    @Test
    void seededProjectsAreStillAvailableAfterMigration() throws SQLException {
        String sql = "SELECT COUNT(*) FROM project WHERE name IN "
                + "('Teamarbeit in Java', 'Praesentation Datenbanken')";

        assertEquals(2, queryInt(sql));
    }

    @Test
    void migratedTasksHaveDescriptionColumnWithData() throws SQLException {
        String sql = "SELECT COUNT(*) FROM task "
                + "WHERE description IS NOT NULL AND LENGTH(description) > 0";

        assertEquals(3, queryInt(sql));
    }

    private int queryInt(String sql) throws SQLException {
        try (var connection = dataSource.getConnection();
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
