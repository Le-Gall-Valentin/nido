package com.nido.api.calendar.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.MigrationDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration 076 changes data already in production: every weekly series gets its anchor's weekday, the only day a
 * weekly series of 0.17 repeats on, and every series the same day of the month. Run here on rows written as 0.17 wrote
 * them.
 */
@IntegrationTestConfig
class CalendarSeriesDayRulesMigrationIT {

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
    void every_weekly_series_written_before_076_repeats_on_its_anchors_weekday_and_the_others_keep_their_day() throws Exception {
        database.migrateUpTo("076-");
        database.execute("INSERT INTO users (username, email, role) VALUES ('alice', 'alice@test.com', 'USER')");
        database.execute("""
            INSERT INTO spaces (type, name_encrypted, accent, glyph, encryption_salt)
            VALUES ('SHARED', 'v3:x', '#8a7d6b', 'x', md5('salt'))""");
        // A Monday, a Thursday and a Sunday weekly; a monthly and a yearly on the 31st.
        for (String[] series : new String[][] {{"WEEKLY", "2026-10-12"}, {"WEEKLY", "2026-10-15"}, {"WEEKLY", "2026-10-18"},
            {"MONTHLY", "2026-10-31"}, {"YEARLY", "2026-12-31"}}) {
            database.execute("""
                INSERT INTO calendar_recurring_event_series
                  (space_id, title_encrypted, all_day, duration_days, interval_type, interval_count, anchor_date, created_by)
                SELECT (SELECT id FROM spaces), 'v3:x', true, 0, '%s', 1, '%s', (SELECT id FROM users)"""
                .formatted(series[0], series[1]));
        }

        database.migrateToTheEnd();

        assertThat(database.strings("""
            SELECT interval_type || ' ' || anchor_date || ' ' || coalesce(weekdays, '-') || ' ' || day_rule
            FROM calendar_recurring_event_series ORDER BY interval_type DESC, anchor_date""")).containsExactly(
            "YEARLY 2026-12-31 - SAME_DAY",
            "WEEKLY 2026-10-12 MO SAME_DAY",
            "WEEKLY 2026-10-15 TH SAME_DAY",
            "WEEKLY 2026-10-18 SU SAME_DAY",
            "MONTHLY 2026-10-31 - SAME_DAY");
    }
}
