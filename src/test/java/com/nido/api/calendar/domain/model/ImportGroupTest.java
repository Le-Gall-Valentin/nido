package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ImportGroupTest {

    @Test
    void everyRowCreatingTheGroupWritesIsCounted() {
        ImportedEvent event = new ImportedEvent("Match", null, null, true, LocalDate.of(2026, 10, 14), null,
            LocalDate.of(2026, 10, 14), null);
        ImportedSeries series = new ImportedSeries("Piano", null, null, true, null, null, 0, RecurrenceInterval.DAILY, 1,
            null, DayRule.SAME_DAY, LocalDate.of(2026, 1, 5), null, null,
            Set.of(LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 8)),
            List.of(new ImportedOccurrence(LocalDate.of(2026, 1, 9), event), new ImportedOccurrence(LocalDate.of(2026, 1, 10), event)));

        // 1 event + 1 series + 2 edited occurrences + 3 cancelled slots
        assertThat(new ImportGroup("a@test", List.of(event), series, false, 0).rows()).isEqualTo(7);
    }
}
