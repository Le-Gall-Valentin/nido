package com.nido.api;

import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migrations 060 and 061 change data that is already in production, so they run here against rows
 * written the way the previous version wrote them: an empty database is migrated up to just before
 * the changeset under test, legacy rows go in, then the rest of the changelog runs.
 *
 * <p>One database per test, created in the shared Postgres container: the shared database is already
 * migrated to the end, and a refused migration must leave nothing behind for the next test.
 */
@IntegrationTestConfig
class IdentifierMigrationIT {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    @Autowired JdbcTemplate sharedDatabase;

    private String databaseName;
    private Connection connection;
    private Liquibase liquibase;

    @BeforeEach
    void createAnEmptyDatabase() throws Exception {
        databaseName = "migration_" + UUID.randomUUID().toString().replace("-", "");
        sharedDatabase.execute("CREATE DATABASE " + databaseName);
        String url = "jdbc:postgresql://" + SharedContainers.POSTGRES.getHost() + ":"
            + SharedContainers.POSTGRES.getMappedPort(5432) + "/" + databaseName;
        connection = DriverManager.getConnection(url, SharedContainers.POSTGRES.getUsername(),
            SharedContainers.POSTGRES.getPassword());
        Database database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
    }

    @AfterEach
    void dropIt() throws Exception {
        liquibase.close();
        sharedDatabase.execute("DROP DATABASE IF EXISTS " + databaseName + " WITH (FORCE)");
    }

    @Test
    void addresses_written_before_060_come_out_in_lower_case() throws Exception {
        migrateUpTo("060-");
        execute("INSERT INTO users (username, email, role) VALUES ('jane', 'Jane.Doe@Example.FR', 'USER')");

        migrateToTheEnd();

        assertThat(strings("SELECT email FROM users WHERE username = 'jane'")).containsExactly("jane.doe@example.fr");
    }

    @Test
    void two_accounts_whose_addresses_differ_only_by_case_stop_the_migration_and_change_nothing() throws Exception {
        migrateUpTo("060-");
        execute("INSERT INTO users (username, email, role) VALUES ('upper', 'Twin@test.com', 'USER')");
        execute("INSERT INTO users (username, email, role) VALUES ('lower', 'twin@test.com', 'USER')");

        assertThatThrownBy(this::migrateToTheEnd).hasStackTraceContaining("two accounts share an email address");
        assertThat(strings("SELECT email FROM users ORDER BY username")).containsExactly("twin@test.com", "Twin@test.com");
    }

    @Test
    void an_account_without_an_address_stops_the_migration() throws Exception {
        migrateUpTo("060-");
        execute("INSERT INTO users (username, email, role) VALUES ('nomail', NULL, 'USER')");

        assertThatThrownBy(this::migrateToTheEnd).hasStackTraceContaining("has no email address");
    }

    @Test
    void an_account_without_a_username_stops_the_migration() throws Exception {
        migrateUpTo("060-");
        execute("INSERT INTO users (username, email, role) VALUES (NULL, 'noname@test.com', 'USER')");

        assertThatThrownBy(this::migrateToTheEnd).hasStackTraceContaining("has no username");
    }

    @Test
    void a_username_holding_an_at_sign_stops_the_migration() throws Exception {
        migrateUpTo("060-");
        execute("INSERT INTO users (username, email, role) VALUES ('jane@home', 'jane@test.com', 'USER')");

        assertThatThrownBy(this::migrateToTheEnd).hasStackTraceContaining("a username contains @");
    }

    @Test
    void anonymized_accounts_pass_untouched() throws Exception {
        migrateUpTo("060-");
        execute("INSERT INTO users (username, email, role, is_deleted, is_active) VALUES (NULL, NULL, 'USER', true, false)");

        migrateToTheEnd();

        assertThat(strings("SELECT coalesce(email, 'none') FROM users")).containsExactly("none");
    }

    @Test
    void a_legacy_invitation_in_any_letter_case_is_bound_to_its_account() throws Exception {
        migrateUpTo("061-");
        execute("INSERT INTO users (username, email, role) VALUES ('jane', 'jane@example.fr', 'USER')");
        execute("INSERT INTO spaces (type, name, accent, glyph, encryption_salt) VALUES ('SHARED', 'Maison', '#8a7d6b', '🏠', md5('salt'))");
        execute("""
            INSERT INTO space_invitations (space_id, email, role, code, status, expires_at)
            SELECT id, 'Jane@Example.FR', 'MEMBER', 'NIDO-AAAAAA', 'PENDING', now() + interval '7 days' FROM spaces""");

        migrateToTheEnd();

        assertThat(strings("""
            SELECT u.username FROM space_invitations i JOIN users u ON u.id = i.invitee_id"""))
            .containsExactly("jane");
    }

    @Test
    void an_invitation_no_account_answers_to_any_more_is_dropped() throws Exception {
        migrateUpTo("061-");
        execute("INSERT INTO users (username, email, role, is_deleted, is_active) VALUES (NULL, NULL, 'USER', true, false)");
        execute("INSERT INTO spaces (type, name, accent, glyph, encryption_salt) VALUES ('SHARED', 'Maison', '#8a7d6b', '🏠', md5('salt'))");
        execute("""
            INSERT INTO space_invitations (space_id, email, role, code, status, expires_at)
            SELECT id, 'gone@example.fr', 'MEMBER', 'NIDO-BBBBBB', 'ACCEPTED', now() FROM spaces""");

        migrateToTheEnd();

        assertThat(strings("SELECT count(*) FROM space_invitations")).containsExactly("0");
        assertThat(strings("""
            SELECT count(*) FROM information_schema.columns
            WHERE table_name = 'space_invitations' AND column_name = 'email'""")).containsExactly("0");
    }

    /** Applies every changeset that comes before the first one whose id starts with the prefix. */
    private void migrateUpTo(String changeSetIdPrefix) throws LiquibaseException {
        List<ChangeSet> changeSets = liquibase.getDatabaseChangeLog().getChangeSets();
        int before = IntStream.range(0, changeSets.size())
            .filter(i -> changeSets.get(i).getId().startsWith(changeSetIdPrefix))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no changeset id starts with " + changeSetIdPrefix));
        liquibase.update(before, "");
    }

    private void migrateToTheEnd() throws LiquibaseException {
        liquibase.update("");
    }

    private void execute(String sql) throws SQLException {
        connection.setAutoCommit(true);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private List<String> strings(String sql) throws SQLException {
        connection.setAutoCommit(true);
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }
}
