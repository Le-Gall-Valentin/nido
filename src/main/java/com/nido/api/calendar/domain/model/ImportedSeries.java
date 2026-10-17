package com.nido.api.calendar.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/** A recurring event of a file that nido can say as a series — checked to give exactly the file's occurrences. */
public record ImportedSeries(String title, String description, String location, boolean allDay,
                             LocalTime startTime, LocalTime endTime, int durationDays,
                             RecurrenceInterval intervalType, int intervalCount, Set<DayOfWeek> weekdays, DayRule dayRule,
                             LocalDate anchorDate, LocalDate startsOn, LocalDate endDate,
                             Set<LocalDate> cancelledSlots, List<ImportedOccurrence> editedOccurrences) {
    public ImportedSeries {
        cancelledSlots = Set.copyOf(cancelledSlots);
        editedOccurrences = List.copyOf(editedOccurrences);
    }
}
