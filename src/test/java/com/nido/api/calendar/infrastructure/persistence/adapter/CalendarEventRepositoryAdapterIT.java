package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class CalendarEventRepositoryAdapterIT {

    @Autowired CalendarEventRepository events;
    @Autowired RecurringEventSeriesRepository series;
    @Autowired EventExclusionRepository exclusions;
    @Autowired SpaceJpaRepository spaces;
    @Autowired UserIdentityJpaRepository users;
    @Autowired JdbcTemplate jdbc;

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
    void storesEveryTextFieldAsCiphertextAndReadsItBack() {
        CalendarEvent created = events.create(new CreateEventCommand(
            spaceId, "RDV oncologue", "Apporter les résultats", "Hôpital Saint-Louis", false,
            LocalDate.of(2026, 3, 2), LocalTime.of(9, 0), LocalDate.of(2026, 3, 2), LocalTime.of(9, 30),
            "accent", List.of(aliceId), null, null, aliceId));

        String storedTitle = jdbc.queryForObject(
            "SELECT title_encrypted FROM calendar_events WHERE id = ?", String.class, created.id());
        String storedLocation = jdbc.queryForObject(
            "SELECT location_encrypted FROM calendar_events WHERE id = ?", String.class, created.id());
        assertThat(storedTitle).doesNotContain("oncologue");
        assertThat(storedLocation).doesNotContain("Saint-Louis");

        assertThat(events.findById(created.id())).get()
            .extracting(CalendarEvent::title, CalendarEvent::description, CalendarEvent::location)
            .containsExactly("RDV oncologue", "Apporter les résultats", "Hôpital Saint-Louis");
        assertThat(created.participantIds()).containsExactly(aliceId);
    }

    @Test
    void leavesANullDescriptionNull() {
        CalendarEvent created = events.create(plainEvent("Courses", LocalDate.of(2026, 3, 2)));
        assertThat(events.findById(created.id())).get()
            .extracting(CalendarEvent::description, CalendarEvent::location)
            .containsExactly(null, null);
    }

    @Test
    void findsAMultiDayEventFromAWindowThatOnlyTouchesItsMiddle() {
        events.create(new CreateEventCommand(
            spaceId, "Vacances", null, null, true,
            LocalDate.of(2026, 7, 1), null, LocalDate.of(2026, 7, 20), null,
            null, List.of(), null, null, aliceId));

        assertThat(events.findBySpaceIdOverlapping(spaceId, LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 11)))
            .extracting(CalendarEvent::title).containsExactly("Vacances");
    }

    @Test
    void doesNotReturnAnEventOfAnotherSpace() {
        SpaceEntity other = new SpaceEntity();
        other.setType(SpaceType.SHARED);
        other.setName("Ailleurs");
        other.setAccent("#4a7fa0");
        other.setGlyph("🏠");
        UUID otherSpaceId = spaces.saveAndFlush(other).getId();

        events.create(new CreateEventCommand(otherSpaceId, "Secret", null, null, true,
            LocalDate.of(2026, 3, 2), null, LocalDate.of(2026, 3, 2), null, null, List.of(), null, null, aliceId));

        assertThat(events.findBySpaceIdOverlapping(spaceId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
            .isEmpty();
    }

    @Test
    void findsADetachedSlotByItsOriginalDateEvenWhenTheEventMovedAway() {
        UUID seriesId = weeklySeries();
        events.create(new CreateEventCommand(
            spaceId, "Piano (décalé)", null, null, false,
            LocalDate.of(2026, 2, 5), LocalTime.of(18, 0), LocalDate.of(2026, 2, 5), LocalTime.of(19, 0),
            null, List.of(), seriesId, LocalDate.of(2026, 2, 3), aliceId));

        // The window covers the ORIGINAL slot but not the new date — the slot must still be freed.
        assertThat(events.findDetachedSlots(seriesId, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 4)))
            .containsExactly(LocalDate.of(2026, 2, 3));
        assertThat(events.findBySeriesAndOriginalDate(seriesId, LocalDate.of(2026, 2, 3)))
            .get().extracting(CalendarEvent::title).isEqualTo("Piano (décalé)");
    }

    @Test
    void replacesParticipantsOnUpdateRatherThanAppending() {
        CalendarEvent created = events.create(new CreateEventCommand(
            spaceId, "Réunion", null, null, true, LocalDate.of(2026, 3, 2), null, LocalDate.of(2026, 3, 2), null,
            null, List.of(aliceId), null, null, aliceId));

        CalendarEvent updated = events.update(new UpdateEventCommand(
            created.id(), "Réunion", null, null, true, LocalDate.of(2026, 3, 2), null, LocalDate.of(2026, 3, 2), null,
            null, List.of()));

        assertThat(updated.participantIds()).isEmpty();
        assertThat(events.findById(created.id())).get()
            .extracting(CalendarEvent::participantIds).isEqualTo(List.of());
    }

    @Test
    void joiningTwiceAddsOneParticipant() {
        CalendarEvent created = events.create(plainEvent("Apéro", LocalDate.of(2026, 3, 2)));
        events.addParticipant(created.id(), aliceId);
        events.addParticipant(created.id(), aliceId);

        assertThat(events.findById(created.id())).get()
            .extracting(CalendarEvent::participantIds).isEqualTo(List.of(aliceId));
    }

    @Test
    void exclusionsAreIdempotentAndClearable() {
        UUID seriesId = weeklySeries();
        exclusions.exclude(seriesId, LocalDate.of(2026, 2, 10));
        exclusions.exclude(seriesId, LocalDate.of(2026, 2, 10));
        assertThat(exclusions.findSlots(seriesId, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
            .containsExactly(LocalDate.of(2026, 2, 10));

        exclusions.clear(seriesId, LocalDate.of(2026, 2, 10));
        assertThat(exclusions.findSlots(seriesId, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))).isEmpty();
    }

    @Test
    void storesTheSeriesTitleAsCiphertextToo() {
        UUID seriesId = weeklySeries();
        String stored = jdbc.queryForObject(
            "SELECT title_encrypted FROM calendar_recurring_event_series WHERE id = ?", String.class, seriesId);
        assertThat(stored).doesNotContain("Piano");
        assertThat(series.findById(seriesId)).get().extracting(s -> s.title()).isEqualTo("Piano");
    }

    private CreateEventCommand plainEvent(String title, LocalDate date) {
        return new CreateEventCommand(spaceId, title, null, null, true, date, null, date, null,
            null, List.of(), null, null, aliceId);
    }

    private UUID weeklySeries() {
        return series.create(new CreateRecurringEventSeriesCommand(
            spaceId, "Piano", null, null, false, LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null,
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 2, 3), null, List.of(), aliceId)).id();
    }
}
