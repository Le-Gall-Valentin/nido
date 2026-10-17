package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.ImportedEvent;
import com.nido.api.calendar.domain.model.ImportedSeries;
import net.fortuna.ical4j.model.component.VEvent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/** What a recurring event of a file becomes. */
final class RecurringImport {

    /** {@code series} when nido can say the rule; {@code events} for the rest; {@code partial} when some were left out. */
    record Result(ImportedSeries series, List<ImportedEvent> events, boolean unfolded, int unfoldedOccurrences, boolean partial) {}

    private final ZoneId zone;
    private final LocalDate today;
    private final Locale locale;

    RecurringImport(ZoneId zone, LocalDate today, Locale locale) {
        this.zone = zone;
        this.today = today;
        this.locale = locale;
    }

    Result translate(VEvent master, List<VEvent> overrides, IcalTexts texts, IcalTimes times) {
        return new Result(null, List.of(Ical4jImportReaderAdapter.event(texts, times)), false, 0, false);
    }
}
