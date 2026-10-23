package com.nido.api.calendar.infrastructure.ical;

import net.fortuna.ical4j.data.DefaultComponentFactorySupplier;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentFactory;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.Observance;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.transform.recurrence.Frequency;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The VTIMEZONEs of an untrusted file, read under a budget. As soon as it reads a zone, whatever its name, ical4j lists
 * every change its observances make from their DTSTART to ten years past today (4.4.0: TimeZone's constructor,
 * ZoneRulesBuilder, Observance.getLatestOnset), and nothing limits it: a zone of a few hundred bytes changing every minute
 * since 1601 fills any heap. So each VTIMEZONE is weighed when its END is read, before ical4j makes anything of it. A
 * zone whose observances do not change on days of the year is no zone a calendar writes, and the changes the zones of one
 * document may list share one budget: either refuses the document. A zone given again is the zone first given, never
 * listed twice — a calendar merged from many invitations repeats theirs. One instance serves one reading of one document.
 */
final class BoundedZones implements ComponentFactory<VTimeZone> {

    /**
     * The changes the zones of one document may list. Outlook writes a zone of some 900 since 1601 and a document may
     * define fifty zones of its own ({@link DocumentZones#MAX_ZONES}): fifty Outlook zones fit, for the next century and
     * more. The longest definition ical4j ships lists under 10 000. Measured on the development machine: a zone of 436
     * bytes spending it all is read in half a second and holds 24 MB for as long as its document is kept; fifty Outlook
     * zones, 0.9 s and 21 MB.
     */
    static final long BUDGET = 60_000;

    /** ical4j lists a zone's changes up to ten years past today (Observance.getLatestOnset)... */
    private static final int YEARS_AHEAD = 10;
    /** ...and those of a zone starting later, up to five years past its start (ZoneRulesBuilder). */
    private static final int YEARS_AFTER_A_LATER_START = 5;
    /** Where ical4j starts an observance that gives no start of its own. */
    private static final int EPOCH_YEAR = 1970;

    private final VTimeZone.Factory typed = new VTimeZone.Factory();
    private final Map<String, VTimeZone> given = new HashMap<>();
    private final int thisYear;
    private long spent;

    /** {@code now}: what ical4j takes as today when it lists a zone's changes, the machine's clock. */
    BoundedZones(Instant now) {
        this.thisYear = now.atZone(ZoneOffset.UTC).getYear();
    }

    /** ical4j's own component factories, with this one in place of its VTIMEZONE's. */
    List<ComponentFactory<?>> factories() {
        return new DefaultComponentFactorySupplier().get().stream()
            .<ComponentFactory<?>>map(factory -> factory.supports(Component.VTIMEZONE) ? this : factory).toList();
    }

    @Override
    public VTimeZone createComponent() {
        return typed.createComponent();
    }

    @Override
    public VTimeZone createComponent(PropertyList properties) {
        return weighed(typed.createComponent(properties));
    }

    @Override
    public VTimeZone createComponent(PropertyList properties, ComponentList<? extends Component> observances) {
        return weighed(typed.createComponent(properties, observances));
    }

    @Override
    public boolean supports(String name) {
        return typed.supports(name);
    }

    private VTimeZone weighed(VTimeZone zone) {
        Optional<String> id = zone.getProperty(Property.TZID).map(Property::getValue);
        if (id.isEmpty()) {
            return zone; // ical4j refuses a zone without a name when it reads it
        }
        VTimeZone first = given.get(id.get());
        if (first != null) {
            return first;
        }
        zone.getObservances().forEach(this::charge);
        given.put(id.get(), zone);
        return zone;
    }

    /**
     * What ical4j lists of one observance: its start, its RDATEs, and every candidate of its rules up to their end. With
     * no start, ical4j lists from 1970 (Observance.getLatestOnset), and so is it billed.
     */
    private void charge(Observance observance) {
        int start = observance.<DtStart<Temporal>>getProperty(Property.DTSTART).map(DtStart::getDate)
            .map(BoundedZones::yearOf).orElse(EPOCH_YEAR);
        bill(1);
        for (RDate<Temporal> rdate : observance.<RDate<Temporal>>getProperties(Property.RDATE)) {
            bill(rdate.getPeriods().map(Set::size).orElseGet(() -> rdate.getDates().size()));
        }
        for (RRule<Temporal> rrule : observance.<RRule<Temporal>>getProperties(Property.RRULE)) {
            Recur<Temporal> recur = rrule.getRecur();
            if (recur.getFrequency() != Frequency.YEARLY || !BoundedExpansion.distinctAndShort(recur)) {
                throw new IllegalStateException("A zone that does not change on days of the year");
            }
            int last = Math.max(thisYear + YEARS_AHEAD, start + YEARS_AFTER_A_LATER_START);
            if (recur.getUntil() != null) {
                last = Math.min(last, yearOf(recur.getUntil()));
            }
            bill(BoundedExpansion.perStep(recur) * Math.max(0, last - start + 1));
        }
    }

    /** At most a budget and one rule's bill: no sum can overflow. */
    private void bill(long changes) {
        spent += changes;
        if (spent > BUDGET) {
            throw new IllegalStateException("Too many changes in the zones of one document");
        }
    }

    private static int yearOf(Temporal temporal) {
        return IcalSupport.toLocal(temporal, ZoneOffset.UTC).getYear();
    }
}
