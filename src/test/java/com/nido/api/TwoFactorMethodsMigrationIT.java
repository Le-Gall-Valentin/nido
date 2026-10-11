package com.nido.api;

import com.nido.api.infrastructure.encryption.LegacyKeys;
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
 * Migration 072 moves the authenticator secrets that protect accounts today. Losing one silently would
 * leave an account with no second factor while its holder believes it has one, so the copy is checked
 * and refused rather than trusted — run here against rows written the way 071 left them.
 */
@IntegrationTestConfig
class TwoFactorMethodsMigrationIT {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";
    private static final String SECRET = "JBSWY3DPEHPK3PXP";

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
    void the_secrets_that_protect_accounts_are_carried_over_untouched_and_nothing_else() throws Exception {
        migrateUpTo("072-");
        UUID jane = account("jane");
        UUID john = account("john");
        String janeSecret = encrypted(jane);
        execute("INSERT INTO user_totp (user_id, totp_secret, totp_enabled) VALUES ('" + jane + "', '" + janeSecret + "', true)");
        execute("INSERT INTO user_totp (user_id, totp_secret, totp_enabled) VALUES ('" + john + "', NULL, false)");

        migrateToTheEnd();

        assertThat(strings("SELECT user_id || ' ' || method || ' ' || secret FROM two_factor_methods"))
            .containsExactly(jane + " APP " + janeSecret);
        assertThat(strings("SELECT count(*) FROM information_schema.tables WHERE table_name = 'user_totp'"))
            .containsExactly("0");
    }

    @Test
    void rolling_back_gives_every_account_the_row_the_version_before_expects() throws Exception {
        // The version before reads one user_totp row per account and only updates it when TOTP is turned on:
        // an account left without its row could never turn TOTP on again after a rollback.
        migrateUpTo("072-");
        UUID jane = account("jane");
        UUID john = account("john");
        String janeSecret = encrypted(jane);
        execute("INSERT INTO user_totp (user_id, totp_secret, totp_enabled) VALUES ('" + jane + "', '" + janeSecret + "', true)");
        execute("INSERT INTO user_totp (user_id, totp_secret, totp_enabled) VALUES ('" + john + "', NULL, false)");
        migrateToTheEnd();

        rollBackDownTo("072-");

        assertThat(strings("SELECT user_id || ' ' || coalesce(totp_secret, '-') || ' ' || totp_enabled FROM user_totp"))
            .containsExactlyInAnyOrder(jane + " " + janeSecret + " true", john + " - false");
    }

    @Test
    void a_carried_over_secret_still_opens_with_its_account_key() throws Exception {
        migrateUpTo("072-");
        UUID jane = account("jane");
        execute("INSERT INTO user_totp (user_id, totp_secret, totp_enabled) VALUES ('" + jane + "', '" + encrypted(jane) + "', true)");

        migrateToTheEnd();

        String stored = strings("SELECT secret FROM two_factor_methods WHERE user_id = '" + jane + "'").getFirst();
        assertThat(LegacyKeys.writer(TestSpaces.ENCRYPTION_KEY, jane.toString().replace("-", "")).decrypt(stored))
            .isEqualTo(SECRET);
    }

    @Test
    void an_account_on_without_a_secret_stops_the_migration_and_changes_nothing() throws Exception {
        migrateUpTo("072-");
        UUID jane = account("jane");
        execute("ALTER TABLE user_totp DROP CONSTRAINT chk_user_totp_secret_required");
        execute("INSERT INTO user_totp (user_id, totp_secret, totp_enabled) VALUES ('" + jane + "', NULL, true)");

        assertThatThrownBy(this::migrateToTheEnd).hasStackTraceContaining("has two-factor authentication on without a secret");
        assertThat(strings("SELECT count(*) FROM user_totp")).containsExactly("1");
        assertThat(strings("SELECT count(*) FROM information_schema.tables WHERE table_name = 'two_factor_methods'"))
            .containsExactly("0");
    }

    @Test
    void the_table_refuses_a_mail_method_with_a_secret_and_an_app_method_without_one() throws Exception {
        migrateToTheEnd();
        UUID jane = account("jane");

        assertThatThrownBy(() -> execute("INSERT INTO two_factor_methods (user_id, method, secret) VALUES ('" + jane + "', 'MAIL', 'x')"))
            .hasMessageContaining("ck_two_factor_methods_secret");
        assertThatThrownBy(() -> execute("INSERT INTO two_factor_methods (user_id, method) VALUES ('" + jane + "', 'APP')"))
            .hasMessageContaining("ck_two_factor_methods_secret");
        assertThatThrownBy(() -> execute("INSERT INTO two_factor_methods (user_id, method) VALUES ('" + jane + "', 'SMS')"))
            .hasMessageContaining("ck_two_factor_methods_method");
    }

    private UUID account(String username) throws SQLException {
        return UUID.fromString(strings("INSERT INTO users (username, email, role) VALUES ('" + username + "', '"
            + username + "@example.fr', 'USER') RETURNING id").getFirst());
    }

    private static String encrypted(UUID user) {
        return LegacyKeys.writer(TestSpaces.ENCRYPTION_KEY, user.toString().replace("-", "")).encrypt(SECRET);
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

    /** Rolls back that changeset and every one after it, however many later versions added. */
    private void rollBackDownTo(String changeSetIdPrefix) throws LiquibaseException {
        List<ChangeSet> changeSets = liquibase.getDatabaseChangeLog().getChangeSets();
        int first = IntStream.range(0, changeSets.size())
            .filter(i -> changeSets.get(i).getId().startsWith(changeSetIdPrefix))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no changeset id starts with " + changeSetIdPrefix));
        liquibase.rollback(changeSets.size() - first, "");
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
