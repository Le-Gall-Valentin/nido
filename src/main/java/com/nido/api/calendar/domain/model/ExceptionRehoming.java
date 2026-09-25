package com.nido.api.calendar.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where the exceptions of a series go once its schedule has changed.
 *
 * <p>An occurrence edited on its own, or cancelled, is something someone chose. Neither is moved nor
 * deleted when the series is edited: each takes the place of the occurrence of the new schedule
 * nearest to the one it replaced, so a week keeps one lesson — the one that was edited — rather
 * than that one plus the new schedule's. Edited occurrences are placed first, the closest first; an
 * occurrence already taken leaves the next one without a place. An edited occurrence without a place
 * stays in the calendar as an event of its own; a cancellation without one no longer cancels anything.
 */
public final class ExceptionRehoming {

    /** Each edited occurrence's new slot, keyed by the slot it replaced; and the slots cancelled now. */
    public record Plan(Map<LocalDate, LocalDate> editedOccurrences, Set<LocalDate> cancellations) {}

    private ExceptionRehoming() {}

    public static Plan plan(RecurringEventSeries target, Collection<LocalDate> edited, Collection<LocalDate> cancelled) {
        Set<LocalDate> taken = new HashSet<>();
        Map<LocalDate, LocalDate> placed = new LinkedHashMap<>();
        for (Candidate candidate : closestFirst(target, edited)) {
            if (taken.add(candidate.slot())) {
                placed.put(candidate.replaced(), candidate.slot());
            }
        }
        Set<LocalDate> cancellations = new LinkedHashSet<>();
        for (Candidate candidate : closestFirst(target, cancelled)) {
            if (taken.add(candidate.slot())) {
                cancellations.add(candidate.slot());
            }
        }
        return new Plan(Collections.unmodifiableMap(placed), Collections.unmodifiableSet(cancellations));
    }

    private record Candidate(LocalDate replaced, LocalDate slot, long distance) {}

    private static List<Candidate> closestFirst(RecurringEventSeries target, Collection<LocalDate> dates) {
        return dates.stream()
            .flatMap(date -> EventRecurrenceProjector.nearestSlot(target, date)
                .map(slot -> new Candidate(date, slot, Math.abs(ChronoUnit.DAYS.between(date, slot)))).stream())
            .sorted(Comparator.comparingLong(Candidate::distance).thenComparing(Candidate::replaced))
            .toList();
    }
}
