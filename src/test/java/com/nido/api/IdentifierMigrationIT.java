package com.nido.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;


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

    @Autowired JdbcTemplate sharedDatabase;

    private MigrationDatabase database;

    @BeforeEach
    void createAnEmptyDatabase() throws Exception {
        database = MigrationDatabase.create(sharedDatabase);
    }

    @AfterEach
    void dropIt() throws Exception {
        database.close();
    }

    @Test
    void addresses_written_before_060_come_out_in_lower_case() throws Exception {
        database.migrateUpTo("060-");
        database.execute("INSERT INTO users (username, email, role) VALUES ('jane', 'Jane.Doe@Example.FR', 'USER')");

        database.migrateToTheEnd();

        assertThat(database.strings("SELECT email FROM users WHERE username = 'jane'")).containsExactly("jane.doe@example.fr");
    }

    @Test
    void two_accounts_whose_addresses_differ_only_by_case_stop_the_migration_and_change_nothing() throws Exception {
        database.migrateUpTo("060-");
        database.execute("INSERT INTO users (username, email, role) VALUES ('upper', 'Twin@test.com', 'USER')");
        database.execute("INSERT INTO users (username, email, role) VALUES ('lower', 'twin@test.com', 'USER')");

        assertThatThrownBy(database::migrateToTheEnd).hasStackTraceContaining("two accounts share an email address");
        assertThat(database.strings("SELECT email FROM users ORDER BY username")).containsExactly("twin@test.com", "Twin@test.com");
    }

    @Test
    void an_account_without_an_address_stops_the_migration() throws Exception {
        database.migrateUpTo("060-");
        database.execute("INSERT INTO users (username, email, role) VALUES ('nomail', NULL, 'USER')");

        assertThatThrownBy(database::migrateToTheEnd).hasStackTraceContaining("has no email address");
    }

    @Test
    void an_account_without_a_username_stops_the_migration() throws Exception {
        database.migrateUpTo("060-");
        database.execute("INSERT INTO users (username, email, role) VALUES (NULL, 'noname@test.com', 'USER')");

        assertThatThrownBy(database::migrateToTheEnd).hasStackTraceContaining("has no username");
    }

    @Test
    void a_username_holding_an_at_sign_stops_the_migration() throws Exception {
        database.migrateUpTo("060-");
        database.execute("INSERT INTO users (username, email, role) VALUES ('jane@home', 'jane@test.com', 'USER')");

        assertThatThrownBy(database::migrateToTheEnd).hasStackTraceContaining("a username contains @");
    }

    @Test
    void anonymized_accounts_pass_untouched() throws Exception {
        database.migrateUpTo("060-");
        database.execute("INSERT INTO users (username, email, role, is_deleted, is_active) VALUES (NULL, NULL, 'USER', true, false)");

        database.migrateToTheEnd();

        assertThat(database.strings("SELECT coalesce(email, 'none') FROM users")).containsExactly("none");
    }

    @Test
    void two_accounts_whose_usernames_differ_only_by_case_stop_062_and_change_nothing() throws Exception {
        database.migrateUpTo("062-");
        database.execute("INSERT INTO users (username, email, role) VALUES ('Jane', 'jane.upper@test.com', 'USER')");
        database.execute("INSERT INTO users (username, email, role) VALUES ('jane', 'jane.lower@test.com', 'USER')");

        assertThatThrownBy(database::migrateToTheEnd).hasStackTraceContaining("two accounts share a username");
        assertThat(database.strings("SELECT username FROM users ORDER BY email")).containsExactly("jane", "Jane");
    }

    @Test
    void usernames_keep_the_case_they_were_given_through_062() throws Exception {
        database.migrateUpTo("062-");
        database.execute("INSERT INTO users (username, email, role) VALUES ('Jane.Doe', 'jane@test.com', 'USER')");
        database.execute("INSERT INTO users (username, email, role, is_deleted, is_active) VALUES ('jane.doe', NULL, 'USER', true, false)");

        database.migrateToTheEnd();

        assertThat(database.strings("SELECT username FROM users WHERE NOT is_deleted")).containsExactly("Jane.Doe");
    }

    @Test
    void a_legacy_invitation_in_any_letter_case_is_bound_to_its_account() throws Exception {
        database.migrateUpTo("061-");
        database.execute("INSERT INTO users (username, email, role) VALUES ('jane', 'jane@example.fr', 'USER')");
        database.execute("INSERT INTO spaces (type, name, accent, glyph, encryption_salt) VALUES ('SHARED', 'Maison', '#8a7d6b', '🏠', md5('salt'))");
        database.execute("""
            INSERT INTO space_invitations (space_id, email, role, code, status, expires_at)
            SELECT id, 'Jane@Example.FR', 'MEMBER', 'NIDO-AAAAAA', 'PENDING', now() + interval '7 days' FROM spaces""");

        database.migrateToTheEnd();

        assertThat(database.strings("""
            SELECT u.username FROM space_invitations i JOIN users u ON u.id = i.invitee_id"""))
            .containsExactly("jane");
    }

    @Test
    void an_invitation_no_account_answers_to_any_more_is_dropped() throws Exception {
        database.migrateUpTo("061-");
        database.execute("INSERT INTO users (username, email, role, is_deleted, is_active) VALUES (NULL, NULL, 'USER', true, false)");
        database.execute("INSERT INTO spaces (type, name, accent, glyph, encryption_salt) VALUES ('SHARED', 'Maison', '#8a7d6b', '🏠', md5('salt'))");
        database.execute("""
            INSERT INTO space_invitations (space_id, email, role, code, status, expires_at)
            SELECT id, 'gone@example.fr', 'MEMBER', 'NIDO-BBBBBB', 'ACCEPTED', now() FROM spaces""");

        database.migrateToTheEnd();

        assertThat(database.strings("SELECT count(*) FROM space_invitations")).containsExactly("0");
        assertThat(database.strings("""
            SELECT count(*) FROM information_schema.columns
            WHERE table_name = 'space_invitations' AND column_name = 'email'""")).containsExactly("0");
    }
}
