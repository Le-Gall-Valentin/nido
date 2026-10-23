package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.RecurringEventSeries;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.zone.ZoneOffsetTransition;
import java.util.List;

/**
 * Whether a series read in a file keeps its hours in the space's zone, when the file wrote it in another one: what the
 * import checks of a rule it would keep as a series, every change of either zone billed to the reading's budget.
 */
final class ZoneShifts {

    /** How long an open-ended series is checked to keep its hours when its zone is not the space's. */
    private static final int OPEN_ENDED_YEARS = 10;

    private final ZoneId zone;
    private final BoundedExpansion expansion;

    ZoneShifts(ZoneId zone, BoundedExpansion expansion) {
        this.zone = zone;
        this.expansion = expansion;
    }

    /**
     * A timed event written in another zone keeps its hours in the space's only while the two zones' offsets keep the same
     * gap — the weeks when the United States and Europe disagree on summer time break it, and no check window of two years
     * is sure to meet them. Walked on both zones' transitions from DTSTART to the series' end, or ten years on.
     */
    boolean keepTheHoursOf(VEvent master, RecurringEventSeries series) {
        Temporal start = master.<DtStart<Temporal>>getProperty(Property.DTSTART).orElseThrow().getDate();
        ZoneId source = switch (start) {
            case ZonedDateTime zoned -> zoned.getZone();
            case OffsetDateTime offset -> offset.getOffset();
            case Instant instant -> ZoneOffset.UTC;
            default -> null;
        };
        if (series.allDay() || source == null || source.getRules().equals(zone.getRules())) {
            return true;
        }
        Instant first = IcalSupport.toLocal(start, zone).atZone(zone).toInstant();
        Instant last = (series.endDate() == null ? first.atZone(zone).plusYears(OPEN_ENDED_YEARS)
            : series.endDate().plusDays(1).atStartOfDay(zone)).toInstant();
        // Past the last transition either zone lists, each zone's rules repeat yearly, but two rules may agree some years
        // and not others ("the Sunday on or after the 24th" against "the last Sunday"). Every kind of year comes round
        // within 28 years: walking that far says it for good, whatever the rules.
        Instant explicitEnd = first;
        for (ZoneId id : List.of(zone, source)) {
            List<ZoneOffsetTransition> listed = id.getRules().getTransitions();
            if (!listed.isEmpty() && listed.getLast().getInstant().isAfter(explicitEnd)) {
                explicitEnd = listed.getLast().getInstant();
            }
        }
        Instant stop = explicitEnd.atZone(ZoneOffset.UTC).plusYears(RecurringImport.RULE_CYCLE_YEARS).toInstant();
        if (stop.isBefore(last)) {
            last = stop;
        }
        int gap = gap(source, first);
        Instant at = first;
        while (true) {
            ZoneOffsetTransition mine = zone.getRules().nextTransition(at);
            ZoneOffsetTransition theirs = source.getRules().nextTransition(at);
            Instant next = mine == null ? (theirs == null ? null : theirs.getInstant())
                : theirs == null || mine.getInstant().isBefore(theirs.getInstant()) ? mine.getInstant() : theirs.getInstant();
            if (next == null || next.isAfter(last)) {
                return true;
            }
            expansion.bill(1);
            if (gap(source, next) != gap) {
                return false;
            }
            at = next;
        }
    }

    private int gap(ZoneId source, Instant at) {
        return source.getRules().getOffset(at).getTotalSeconds() - zone.getRules().getOffset(at).getTotalSeconds();
    }
}
