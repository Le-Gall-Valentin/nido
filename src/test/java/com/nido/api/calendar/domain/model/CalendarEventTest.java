package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarEventTest {

    @Test
    void refusesAnAllDayEventCarryingTimes() {
        assertThatThrownBy(() -> event(true, LocalTime.of(10, 0), LocalTime.of(11, 0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("allDay");
    }

    @Test
    void refusesATimedEventWithoutTimes() {
        assertThatThrownBy(() -> event(false, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("allDay");
    }

    @Test
    void refusesAnEventWithOnlyOneOfItsTwoTimes() {
        assertThatThrownBy(() -> event(false, LocalTime.of(10, 0), null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesAnEventEndingBeforeItStarts() {
        assertThatThrownBy(() -> new CalendarEvent(
            UUID.randomUUID(), UUID.randomUUID(), "t", null, null, true,
            LocalDate.of(2026, 1, 10), null, LocalDate.of(2026, 1, 1), null,
            null, List.of(), null, null, UUID.randomUUID(), Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("endDate");
    }

    @Test
    void acceptsAMultiDayAllDayEvent() {
        assertThatCode(() -> new CalendarEvent(
            UUID.randomUUID(), UUID.randomUUID(), "Vacances", null, null, true,
            LocalDate.of(2026, 7, 1), null, LocalDate.of(2026, 7, 20), null,
            null, List.of(), null, null, UUID.randomUUID(), Instant.now()))
            .doesNotThrowAnyException();
    }

    @Test
    void refusesHalfADetachment() {
        assertThatThrownBy(() -> new CalendarEvent(
            UUID.randomUUID(), UUID.randomUUID(), "t", null, null, true,
            LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 1, 1), null,
            null, List.of(), UUID.randomUUID(), null, UUID.randomUUID(), Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("detachment");
    }

    @Test
    void recognisesADetachedOccurrence() {
        CalendarEvent detached = new CalendarEvent(
            UUID.randomUUID(), UUID.randomUUID(), "t", null, null, true,
            LocalDate.of(2026, 1, 5), null, LocalDate.of(2026, 1, 5), null,
            null, List.of(), UUID.randomUUID(), LocalDate.of(2026, 1, 3), UUID.randomUUID(), Instant.now());

        assertThat(detached.isDetachedOccurrence()).isTrue();
        // The original date is the slot replaced, not where the event now sits.
        assertThat(detached.recurringOriginalDate()).isNotEqualTo(detached.startDate());
    }

    @Test
    void aPlainEventIsNotADetachedOccurrence() {
        assertThat(event(true, null, null).isDetachedOccurrence()).isFalse();
    }

    private CalendarEvent event(boolean allDay, LocalTime start, LocalTime end) {
        return new CalendarEvent(
            UUID.randomUUID(), UUID.randomUUID(), "t", null, null, allDay,
            LocalDate.of(2026, 1, 1), start, LocalDate.of(2026, 1, 1), end,
            null, List.of(), null, null, UUID.randomUUID(), Instant.now());
    }
}
