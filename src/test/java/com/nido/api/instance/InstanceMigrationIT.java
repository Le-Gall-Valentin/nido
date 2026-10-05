package com.nido.api.instance;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.SharedContainers;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 064 decides, for every database already in production, whether its installation is done. */
@IntegrationTestConfig
class InstanceMigrationIT {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    @Autowired JdbcTemplate sharedDatabase;

    private String databaseName;
    private Connection connection;
    private Liquibase liquibase;

    @BeforeEach
    void createAnEmptyDatabase() throws Exception {
        databaseName = "migration_" + UUID.randomUUID().toString().replace("-", "");
        sharedDatabase.execute("CREATE DATABASE " + databaseName);
        connection = DriverManager.getConnection(SharedContainers.jdbcUrl(databaseName),
            SharedContainers.POSTGRES.getUsername(), SharedContainers.POSTGRES.getPassword());
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
    }

    @AfterEach
    void dropIt() throws Exception {
        liquibase.close();
        sharedDatabase.execute("DROP DATABASE IF EXISTS " + databaseName + " WITH (FORCE)");
    }

    @Test
    void a_database_with_accounts_is_an_installation_already_set_up() throws Exception {
        migrateUpTo("064-");
        execute("INSERT INTO users (username, email, role) VALUES ('jane', 'jane@example.fr', 'SUPER_ADMIN')");

        liquibase.update("");

        assertThat(query("SELECT setup_completed_at IS NOT NULL FROM instance")).containsExactly("t");
        assertThat(query("SELECT key_fingerprint IS NULL FROM instance")).containsExactly("t");
    }

    @Test
    void an_empty_database_waits_for_its_setup() throws Exception {
        liquibase.update("");

        assertThat(query("SELECT setup_completed_at IS NULL FROM instance")).containsExactly("t");
    }

    @Test
    void there_is_never_a_second_row() throws Exception {
        liquibase.update("");

        assertThatThrownBy(() -> execute("INSERT INTO instance (id, created_at) VALUES (2, now())"))
            .hasMessageContaining("ck_instance_single_row");
    }

    private void migrateUpTo(String prefix) throws Exception {
        List<ChangeSet> changeSets = liquibase.getDatabaseChangeLog().getChangeSets();
        int before = IntStream.range(0, changeSets.size())
            .filter(i -> changeSets.get(i).getId().startsWith(prefix))
            .findFirst().orElseThrow();
        liquibase.update(before, "");
    }

    private void execute(String sql) throws Exception {
        connection.setAutoCommit(true);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private List<String> query(String sql) throws Exception {
        connection.setAutoCommit(true);
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            List<String> values = new java.util.ArrayList<>();
            while (rows.next()) {
                values.add(rows.getString(1));
            }
            return values;
        }
    }
}
