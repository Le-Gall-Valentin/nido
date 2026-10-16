package com.nido.api.calendar.infrastructure.ical;

import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.Map;
import java.util.Optional;

/**
 * The zones of one document being read. A name the JDK knows is read with the JDK's rules, because ical4j 4.3.0 builds a
 * zone's rules from its VTIMEZONE and puts the March change of "the second Sunday" on the first one: events of a week
 * of March would come an hour off. Any other name keeps its VTIMEZONE, read by ical4j. The registry lives and dies with
 * its document: no document's zones reach another, and the ids ical4j draws from its global pool of 1500 for each zone
 * it registers are given back when the registry is collected, instead of running out over the life of the server.
 */
final class DocumentZones implements TimeZoneRegistry {

    /** More own zones than a real document has: a calendar application writes one or two. */
    static final int MAX_ZONES = 50;

    /** Never empty: ical4j's TzId only asks this registry when it has rules, else it goes to its own (faulty) zones. */
    private static final Map<String, ZoneRules> NON_EMPTY = Map.of("nido", ZoneOffset.UTC.getRules());

    private final TimeZoneRegistry inner = TimeZoneRegistryFactory.getInstance().createRegistry();
    private int ownZones;

    static Optional<ZoneId> jdkZone(String tzId) {
        if (tzId == null || tzId.startsWith("ical4j")) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZoneId.of(tzId));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public void register(TimeZone timezone) {
        register(timezone, false);
    }

    @Override
    public void register(TimeZone timezone, boolean update) {
        if (jdkZone(timezone.getID()).isPresent()) {
            return; // the JDK reads that name
        }
        if (++ownZones > MAX_ZONES) {
            throw new IllegalStateException("Too many time zones in one document");
        }
        inner.register(timezone, update);
    }

    @Override
    public void clear() {
        inner.clear();
    }

    @Override
    public TimeZone getTimeZone(String id) {
        return inner.getTimeZone(id);
    }

    /** Only TzId reads this map (ical4j's zone-rules provider reads the inner registry's, never this one). */
    @Override
    public Map<String, ZoneRules> getZoneRules() {
        Map<String, ZoneRules> rules = inner.getZoneRules();
        return rules.isEmpty() ? NON_EMPTY : rules;
    }

    @Override
    public ZoneId getZoneId(String tzId) {
        return jdkZone(tzId).orElseGet(() -> inner.getZoneId(tzId));
    }

    @Override
    public String getTzId(String alias) {
        return inner.getTzId(alias);
    }
}
