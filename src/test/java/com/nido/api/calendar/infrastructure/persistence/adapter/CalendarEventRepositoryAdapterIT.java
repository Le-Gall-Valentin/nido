package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CreateEventCommand;
import com.nido.api.calendar.domain.model.CreateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import com.nido.api.calendar.domain.model.UpdateEventCommand;
import com.nido.api.calendar.domain.model.UpdateRecurringEventSeriesCommand;
import com.nido.api.calendar.domain.port.out.CalendarEventRepository;
import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.domain.port.out.ImportedUidRepository;
import com.nido.api.calendar.domain.port.out.RecurringEventSeriesRepository;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventEntity;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarRecurringEventSeriesEntity;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTestConfig
class CalendarEventRepositoryAdapterIT {

    @Autowired CalendarEventRepository events;
    @Autowired RecurringEventSeriesRepository series;
    @Autowired EventExclusionRepository exclusions;
    @Autowired ImportedUidRepository importedUids;
    @Autowired SpaceJpaRepository spaces;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceSealers sealers;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    private UUID spaceId;
    private UUID aliceId;
    private final List<UUID> madeSpaces = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, "Colocation");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaces.saveAndFlush(space).getId();
        madeSpaces.add(spaceId);

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("alice-" + UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@test.com");
        user.setRole(Role.USER);
        aliceId = users.saveAndFlush(user).getId();
    }

    @AfterEach
    void cleanUp() {
        // The database is shared with every other test: only what this one made goes, its calendar with its spaces.
        madeSpaces.forEach(spaces::deleteById);
        users.deleteById(aliceId);
    }

    @Test
    void storesEveryTextFieldAsCiphertextAndReadsItBack() {
        CalendarEvent created = events.create(new CreateEventCommand(
            spaceId, "RDV oncologue", "Apporter les résultats", "Hôpital Saint-Louis", false,
            LocalDate.of(2026, 3, 2), LocalTime.of(9, 0), LocalDate.of(2026, 3, 2), LocalTime.of(9, 30),
            "accent", List.of(aliceId), null, null, aliceId));

        String storedTitle = jdbc.queryForObject(
            "SELECT title_encrypted FROM calendar_events WHERE id = ?", String.class, created.id());
        String storedDescription = jdbc.queryForObject(
            "SELECT description_encrypted FROM calendar_events WHERE id = ?", String.class, created.id());
        String storedLocation = jdbc.queryForObject(
            "SELECT location_encrypted FROM calendar_events WHERE id = ?", String.class, created.id());
        assertThat(storedTitle).startsWith("v3:").doesNotContain("oncologue");
        assertThat(storedDescription).doesNotContain("résultats");
        assertThat(storedLocation).doesNotContain("Saint-Louis");
        assertThat(sealers.forSpace(spaceId).open(CalendarEventEntity.TITLE, created.id(), storedTitle)).isEqualTo("RDV oncologue");
        assertThat(sealers.forSpace(spaceId).open(CalendarEventEntity.DESCRIPTION, created.id(), storedDescription))
            .isEqualTo("Apporter les résultats");
        assertThat(sealers.forSpace(spaceId).open(CalendarEventEntity.LOCATION, created.id(), storedLocation)).isEqualTo("Hôpital Saint-Louis");

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
        TestSpaces.name(other, "Ailleurs");
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
        assertThat(events.findDetachedSlotsOfEach(List.of(seriesId), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 4)))
            .containsExactly(Map.entry(seriesId, Set.of(LocalDate.of(2026, 2, 3))));
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
    void anUpdatedEventReadsBackItsDescriptionAndPlace() {
        CalendarEvent created = events.create(plainEvent("Dentiste", LocalDate.of(2026, 3, 2)));

        events.update(new UpdateEventCommand(
            created.id(), "Dentiste", "Détartrage", "Cabinet du Dr Martin", true, LocalDate.of(2026, 3, 2), null,
            LocalDate.of(2026, 3, 2), null, null, List.of()));

        assertThat(events.findById(created.id())).get()
            .extracting(CalendarEvent::description, CalendarEvent::location)
            .containsExactly("Détartrage", "Cabinet du Dr Martin");
    }

    @Test
    void aSeriesKeepsItsDescriptionAndPlaceWhenCreatedAndWhenUpdated() {
        UUID seriesId = series.create(new CreateRecurringEventSeriesCommand(
            spaceId, "Piano", "Solfège", "Conservatoire", false, LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null,
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 2, 3), null, List.of(), aliceId)).id();
        assertThat(series.findById(seriesId)).get()
            .extracting(RecurringEventSeries::description, RecurringEventSeries::location)
            .containsExactly("Solfège", "Conservatoire");

        series.update(new UpdateRecurringEventSeriesCommand(
            seriesId, "Piano", "Gammes", "Salle 2", false, LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null,
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 2, 3), null, List.of()));

        assertThat(series.findById(seriesId)).get()
            .extracting(RecurringEventSeries::description, RecurringEventSeries::location)
            .containsExactly("Gammes", "Salle 2");
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
        assertThat(exclusions.findSlotsOfEach(List.of(seriesId), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
            .containsExactly(Map.entry(seriesId, Set.of(LocalDate.of(2026, 2, 10))));

        exclusions.clear(seriesId, LocalDate.of(2026, 2, 10));
        assertThat(exclusions.findSlotsOfEach(List.of(seriesId), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))).isEmpty();
    }

    // A double click sends the same request twice at once. The second used to check, find nothing yet,
    // insert, and fail on the unique constraint once the first committed — a 500 for a harmless repeat.

    @Test
    void joiningTwiceAtTheSameMomentAddsOneParticipantAndFailsNeither() throws Exception {
        CalendarEvent created = events.create(plainEvent("Apéro", LocalDate.of(2026, 3, 2)));

        assertThat(atTheSameMoment(() -> events.addParticipant(created.id(), aliceId))).isEmpty();
        assertThat(events.findById(created.id())).get()
            .extracting(CalendarEvent::participantIds).isEqualTo(List.of(aliceId));
    }

    @Test
    void cancellingOneOccurrenceTwiceAtTheSameMomentCancelsItOnceAndFailsNeither() throws Exception {
        UUID seriesId = weeklySeries();

        assertThat(atTheSameMoment(() -> exclusions.exclude(seriesId, LocalDate.of(2026, 2, 10)))).isEmpty();
        assertThat(exclusions.findAllSlots(seriesId)).containsExactly(LocalDate.of(2026, 2, 10));
    }

    /**
     * Runs {@code write} in two transactions overlapping the way two requests do: the second starts
     * while the first has written and not committed yet, then the first commits. Returns what failed.
     */
    private List<Throwable> atTheSameMoment(Runnable write) throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CountDownLatch firstWrote = new CountDownLatch(1);
        CountDownLatch firstMayCommit = new CountDownLatch(1);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = threads.submit(() -> transaction.executeWithoutResult(status -> {
                write.run();
                firstWrote.countDown();
                awaitQuietly(firstMayCommit);
            }));
            assertThat(firstWrote.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> second = threads.submit(() -> transaction.executeWithoutResult(status -> write.run()));
            // Long enough for the second to have checked and started writing, which then waits on the first.
            Thread.sleep(500);
            firstMayCommit.countDown();
            for (Future<?> request : List.of(first, second)) {
                try {
                    request.get(10, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
        } finally {
            threads.shutdownNow();
        }
        return failures;
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void storesTheSeriesTitleAsCiphertextToo() {
        UUID seriesId = weeklySeries();
        String stored = jdbc.queryForObject(
            "SELECT title_encrypted FROM calendar_recurring_event_series WHERE id = ?", String.class, seriesId);
        assertThat(stored).startsWith("v3:").doesNotContain("Piano");
        assertThat(sealers.forSpace(spaceId).open(CalendarRecurringEventSeriesEntity.TITLE, seriesId, stored)).isEqualTo("Piano");
        assertThat(series.findById(seriesId)).get().extracting(s -> s.title()).isEqualTo("Piano");
    }

    @Test
    void a_series_stores_its_description_and_place_sealed_to_their_own_columns() {
        UUID seriesId = series.create(new CreateRecurringEventSeriesCommand(
            spaceId, "Piano", "Solfège", "Conservatoire", false, LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null,
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 2, 3), null, List.of(), aliceId)).id();
        SpaceSealer sealer = sealers.forSpace(spaceId);

        String description = jdbc.queryForObject(
            "SELECT description_encrypted FROM calendar_recurring_event_series WHERE id = ?", String.class, seriesId);
        String location = jdbc.queryForObject(
            "SELECT location_encrypted FROM calendar_recurring_event_series WHERE id = ?", String.class, seriesId);
        assertThat(sealer.open(CalendarRecurringEventSeriesEntity.DESCRIPTION, seriesId, description)).isEqualTo("Solfège");
        assertThat(sealer.open(CalendarRecurringEventSeriesEntity.LOCATION, seriesId, location)).isEqualTo("Conservatoire");
    }

    @Test
    void a_title_copied_from_another_event_is_refused() {
        CalendarEvent dentist = events.create(plainEvent("Dentiste", LocalDate.of(2026, 3, 2)));
        CalendarEvent piano = events.create(plainEvent("Piano", LocalDate.of(2026, 3, 3)));
        jdbc.update("UPDATE calendar_events SET title_encrypted = (SELECT title_encrypted FROM calendar_events WHERE id = ?) WHERE id = ?",
            dentist.id(), piano.id());

        assertThatThrownBy(() -> events.findById(piano.id())).isInstanceOf(SealedValueRejected.class);
    }

    @Test
    void an_imported_uid_is_known_in_its_space_until_its_rows_are_deleted() {
        String hash = CalendarDigests.sha256Hex("abc@google.com");
        CalendarEvent event = events.createImported(new CreateEventCommand(spaceId, "Match", null, null, true,
            LocalDate.of(2026, 10, 14), null, LocalDate.of(2026, 10, 14), null, null, List.of(), null, null, aliceId), hash);
        RecurringEventSeries weekly = series.createImported(new CreateRecurringEventSeriesCommand(spaceId, "Entraînement",
            null, null, true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 10, 12), null,
            List.of(), aliceId), CalendarDigests.sha256Hex("def@google.com"));

        assertThat(importedUids.knownHashes(spaceId, List.of(hash, CalendarDigests.sha256Hex("def@google.com"), "other")))
            .containsExactlyInAnyOrder(hash, CalendarDigests.sha256Hex("def@google.com"));
        assertThat(importedUids.knownHashes(UUID.randomUUID(), List.of(hash))).as("another space").isEmpty();

        events.delete(event.id());
        series.delete(weekly.id());
        assertThat(importedUids.knownHashes(spaceId, List.of(hash))).isEmpty();
    }

    @Test
    void the_ids_of_a_nido_export_are_recognised_in_their_space_only() {
        CalendarEvent event = events.create(new CreateEventCommand(spaceId, "Concert", null, null, true,
            LocalDate.of(2026, 9, 23), null, LocalDate.of(2026, 9, 23), null, null, List.of(), null, null, aliceId));
        UUID unknown = UUID.randomUUID();

        assertThat(importedUids.existingIds(spaceId, List.of(event.id(), unknown))).containsExactly(event.id());
        assertThat(importedUids.existingIds(UUID.randomUUID(), List.of(event.id()))).isEmpty();
    }

    @Test
    void a_copy_of_an_imported_event_is_not_imported_itself() {
        String hash = CalendarDigests.sha256Hex("abc@google.com");
        CalendarEvent event = events.createImported(new CreateEventCommand(spaceId, "Match", null, null, true,
            LocalDate.of(2026, 10, 14), null, LocalDate.of(2026, 10, 14), null, null, List.of(), null, null, aliceId), hash);
        events.create(new CreateEventCommand(spaceId, event.title(), null, null, true, event.startDate(), null,
            event.endDate(), null, null, List.of(), null, null, aliceId));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_events WHERE space_id = ? AND import_uid_hash = ?",
            Long.class, spaceId, hash)).isOne();
    }

    @Test
    void every_event_of_a_space_is_found_with_its_edited_occurrences_and_nothing_of_another_space() {
        UUID weekly = weeklySeries();
        CalendarEvent concert = events.create(plainEvent("Concert", LocalDate.of(2026, 9, 23)));
        CalendarEvent edited = events.create(new CreateEventCommand(spaceId, "Piano (décalé)", null, null, false,
            LocalDate.of(2026, 2, 11), LocalTime.of(19, 0), LocalDate.of(2026, 2, 11), LocalTime.of(20, 0), null, List.of(),
            weekly, LocalDate.of(2026, 2, 10), aliceId));
        UUID elsewhere = newSpace();
        events.create(new CreateEventCommand(elsewhere, "Ailleurs", null, null, true, LocalDate.of(2026, 9, 23), null,
            LocalDate.of(2026, 9, 23), null, null, List.of(), null, null, aliceId));

        assertThat(events.findBySpaceId(spaceId)).extracting(CalendarEvent::id, CalendarEvent::title)
            .containsExactlyInAnyOrder(tuple(concert.id(), "Concert"), tuple(edited.id(), "Piano (décalé)"));
    }

    @Test
    void the_cancelled_slots_of_several_series_are_read_at_once_and_a_series_without_any_is_absent() {
        UUID first = weeklySeries();
        UUID second = weeklySeries();
        UUID untouched = weeklySeries();
        exclusions.exclude(first, LocalDate.of(2026, 2, 10));
        exclusions.exclude(first, LocalDate.of(2026, 2, 17));
        exclusions.exclude(second, LocalDate.of(2026, 2, 24));

        assertThat(exclusions.findAllSlotsOfEach(List.of(first, second, untouched))).isEqualTo(Map.of(
            first, Set.of(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 17)),
            second, Set.of(LocalDate.of(2026, 2, 24))));
        assertThat(exclusions.findAllSlotsOfEach(List.of())).isEmpty();
    }

    private UUID newSpace() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, "Ailleurs");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        UUID id = spaces.saveAndFlush(space).getId();
        madeSpaces.add(id);
        return id;
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
