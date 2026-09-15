package com.nido.api.calendar.infrastructure.persistence;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The calendar's invariants are carried by the schema, not only by the domain records, so that
 * no future code path — a bulk import, a repair script, a later handler — can write a row the
 * projector cannot interpret. These tests bypass JPA entirely and go straight to SQL, because
 * going through the domain would only re-test the domain's own guards.
 */
@IntegrationTestConfig
class CalendarSchemaIT {

    @Autowired JdbcTemplate jdbc;
    @Autowired SpaceJpaRepository spaces;
    @Autowired UserIdentityJpaRepository users;

    private UUID spaceId;
    private UUID aliceId;

    @BeforeEach
    void setUp() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Colocation");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaces.saveAndFlush(space).getId();

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("alice-" + UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@test.com");
        user.setRole(Role.USER);
        aliceId = users.saveAndFlush(user).getId();
    }

    @Test
    void rejectsAnAllDayEventThatStillCarriesTimes() {
        assertThatThrownBy(() -> insertEvent(
            "true", "'2026-01-01'", "'10:00'", "'2026-01-01'", "'11:00'", "NULL", "NULL"))
            .hasMessageContaining("chk_ce_all_day");
    }

    @Test
    void rejectsATimedEventMissingItsTimes() {
        assertThatThrownBy(() -> insertEvent(
            "false", "'2026-01-01'", "NULL", "'2026-01-01'", "NULL", "NULL", "NULL"))
            .hasMessageContaining("chk_ce_all_day");
    }

    @Test
    void rejectsAnEventEndingBeforeItStarts() {
        assertThatThrownBy(() -> insertEvent(
            "true", "'2026-01-10'", "NULL", "'2026-01-01'", "NULL", "NULL", "NULL"))
            .hasMessageContaining("chk_ce_dates");
    }

    @Test
    void rejectsHalfADetachment() {
        UUID seriesId = insertSeries();
        assertThatThrownBy(() -> insertEvent(
            "true", "'2026-01-01'", "NULL", "'2026-01-01'", "NULL", "'" + seriesId + "'", "NULL"))
            .hasMessageContaining("chk_ce_detachment");
    }

    @Test
    void acceptsAWholeDetachment() {
        UUID seriesId = insertSeries();
        assertThatCode(() -> insertEvent(
            "true", "'2026-01-05'", "NULL", "'2026-01-05'", "NULL", "'" + seriesId + "'", "'2026-01-03'"))
            .doesNotThrowAnyException();
    }

    @Test
    void refusesTwoDetachedInstancesForTheSameSlot() {
        UUID seriesId = insertSeries();
        insertEvent("true", "'2026-01-05'", "NULL", "'2026-01-05'", "NULL", "'" + seriesId + "'", "'2026-01-03'");
        assertThatThrownBy(() -> insertEvent(
            "true", "'2026-01-06'", "NULL", "'2026-01-06'", "NULL", "'" + seriesId + "'", "'2026-01-03'"))
            .hasMessageContaining("uq_calendar_events_series_slot");
    }

    @Test
    void rejectsASeriesEndingBeforeItsAnchor() {
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO calendar_recurring_event_series
              (space_id, title_encrypted, all_day, duration_days, interval_type, interval_count, anchor_date, end_date, created_by)
            VALUES (?, 'x', true, 0, 'WEEKLY', 1, '2026-02-01', '2026-01-01', ?)
            """, spaceId, aliceId))
            .hasMessageContaining("chk_cres_end_date");
    }

    @Test
    void rejectsAZeroInterval() {
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO calendar_recurring_event_series
              (space_id, title_encrypted, all_day, duration_days, interval_type, interval_count, anchor_date, created_by)
            VALUES (?, 'x', true, 0, 'WEEKLY', 0, '2026-01-01', ?)
            """, spaceId, aliceId))
            .hasMessageContaining("chk_cres_interval_count");
    }

    @Test
    void cascadesExclusionsAndDetachedInstancesWhenASeriesIsDeleted() {
        UUID seriesId = insertSeries();
        insertEvent("true", "'2026-01-05'", "NULL", "'2026-01-05'", "NULL", "'" + seriesId + "'", "'2026-01-03'");
        jdbc.update("INSERT INTO calendar_event_exclusions (series_id, original_date) VALUES (?, '2026-01-10')", seriesId);

        jdbc.update("DELETE FROM calendar_recurring_event_series WHERE id = ?", seriesId);

        assertThatCode(() -> {
            Integer events = jdbc.queryForObject(
                "SELECT count(*) FROM calendar_events WHERE recurring_series_id = ?", Integer.class, seriesId);
            Integer exclusions = jdbc.queryForObject(
                "SELECT count(*) FROM calendar_event_exclusions WHERE series_id = ?", Integer.class, seriesId);
            org.assertj.core.api.Assertions.assertThat(events).isZero();
            org.assertj.core.api.Assertions.assertThat(exclusions).isZero();
        }).doesNotThrowAnyException();
    }

    private UUID insertSeries() {
        jdbc.update("""
            INSERT INTO calendar_recurring_event_series
              (space_id, title_encrypted, all_day, duration_days, interval_type, interval_count, anchor_date, created_by)
            VALUES (?, 'x', true, 0, 'WEEKLY', 1, '2026-01-03', ?)
            """, spaceId, aliceId);
        return jdbc.queryForObject(
            "SELECT id FROM calendar_recurring_event_series WHERE space_id = ? ORDER BY created_at DESC LIMIT 1",
            UUID.class, spaceId);
    }

    private void insertEvent(String allDay, String startDate, String startTime, String endDate, String endTime,
                             String seriesId, String originalDate) {
        jdbc.update(("""
            INSERT INTO calendar_events
              (space_id, title_encrypted, all_day, start_date, start_time, end_date, end_time,
               recurring_series_id, recurring_original_date, created_by)
            VALUES (?, 'x', %s, %s, %s, %s, %s, %s, %s, ?)
            """).formatted(allDay, startDate, startTime, endDate, endTime, seriesId, originalDate),
            spaceId, aliceId);
    }
}
