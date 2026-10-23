package com.nido.api.calendar.infrastructure.ical;

import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.ZoneRulesBuilder;
import net.fortuna.ical4j.model.parameter.TzId;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The zones of one document being read. A name the JDK knows — itself, or after a global prefix such as Lightning's
 * {@code /mozilla.org/20070129_1/} — is read with the JDK's rules, whatever the document says: they hold the zone's
 * whole history, where a file's definition often holds only today's rule (Outlook's start in 1601). A name ical4j's alias
 * table gives a JDK zone for, such as Outlook's "Romance Standard Time", is read with that zone's rules too, but only when
 * the document defines it alike, or does not define it: the table is wrong for a few names ("Samoa Standard Time" is Pago
 * Pago in it, a day away from Apia), and the document then says best. Any other name keeps its VTIMEZONE, read by ical4j.
 * The registry lives and dies with its document: no document's zones reach another, and the ids ical4j draws from its
 * global pool of 1500 for each zone it registers are given back when the registry is collected, instead of running out
 * over the life of the server.
 */
final class DocumentZones implements TimeZoneRegistry {

    /** More own zones than a real document has: a calendar application writes one or two. */
    static final int MAX_ZONES = 50;

    /** Longer than any zone's name, prefixed or not (the JDK's longest is 32 characters). */
    static final int MAX_NAME_LENGTH = 256;
    /** The JDK's names hold at most three parts: America/Argentina/Buenos_Aires. */
    private static final int MAX_NAME_PARTS = 3;

    /** Never empty: ical4j's TzId only asks this registry when it has rules, else it goes to its own zones. */
    private static final Map<String, ZoneRules> NON_EMPTY = Map.of("nido", ZoneOffset.UTC.getRules());

    private final TimeZoneRegistry inner = TimeZoneRegistryFactory.getInstance().createRegistry();
    private final Set<String> ownZones = new HashSet<>();
    /** The names the document defines as the JDK zone ical4j's alias table gives them. */
    private final Map<String, ZoneId> agreed = new HashMap<>();
    private final Instant now;

    /** {@code now}: what ical4j takes as today when it builds a zone's rules, the machine's clock. */
    DocumentZones(Instant now) {
        this.now = now;
    }

    /**
     * The JDK zone of a name it knows, itself or after a global prefix ({@code /vendor/version/Area/City}). ical4j asks
     * it at every reading of a date, so a name longer than any zone's is nobody's, and only the last parts of a prefixed
     * one are tried: a file cannot make each reading cost the square of its length.
     */
    static Optional<ZoneId> jdkZone(String tzId) {
        if (tzId == null || tzId.length() > MAX_NAME_LENGTH) {
            return Optional.empty();
        }
        Optional<ZoneId> named = known(tzId);
        if (named.isPresent() || !tzId.startsWith(TzId.PREFIX)) {
            return named;
        }
        // The starts of its last parts, the longest name first.
        Deque<Integer> starts = new ArrayDeque<>();
        for (int slash = tzId.lastIndexOf('/'); slash >= 0 && starts.size() < MAX_NAME_PARTS;
             slash = tzId.lastIndexOf('/', slash - 1)) {
            starts.push(slash + 1);
        }
        for (int start : starts) {
            named = known(tzId.substring(start));
            if (named.isPresent()) {
                return named;
            }
        }
        return Optional.empty();
    }

    private static Optional<ZoneId> known(String id) {
        // ical4j registers ids of its own with the JDK, for the zones it builds: a file's name is never one of them.
        if (id.isEmpty() || id.startsWith("ical4j")) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZoneId.of(id));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    /** The JDK zone ical4j's alias table gives a name (Windows names, old ids), filled when its registry was made. */
    private static Optional<ZoneId> aliased(String tzId) {
        return Optional.ofNullable(ZONE_ALIASES.get(tzId)).flatMap(DocumentZones::known);
    }

    @Override
    public void register(TimeZone timezone) {
        register(timezone, false);
    }

    @Override
    public void register(TimeZone timezone, boolean update) {
        String id = timezone.getID();
        if (jdkZone(id).isPresent()) {
            return; // the JDK reads that name
        }
        // A zone given again keeps its first definition: ical4j would draw another id of its pool for each repetition.
        if (!ownZones.add(id)) {
            return;
        }
        if (ownZones.size() > MAX_ZONES) {
            throw new IllegalStateException("Too many time zones in one document");
        }
        Optional<ZoneId> alike = aliased(id)
            .filter(alias -> sameOffsets(alias.getRules(), new ZoneRulesBuilder().vTimeZone(timezone.getVTimeZone()).build()));
        if (alike.isPresent()) {
            agreed.put(id, alike.get());
            return;
        }
        inner.register(timezone, update);
    }

    /**
     * Whether two rules give the same offset at every instant from today on — walked on both rules' changes over the 28
     * years in which every kind of year comes round, after which yearly rules repeat.
     */
    private boolean sameOffsets(ZoneRules one, ZoneRules other) {
        Instant end = now.atZone(ZoneOffset.UTC).plusYears(RecurringImport.RULE_CYCLE_YEARS).toInstant();
        for (Instant at = now; at != null && at.isBefore(end); at = nextChange(one, other, at)) {
            if (!one.getOffset(at).equals(other.getOffset(at))) {
                return false;
            }
        }
        return true;
    }

    private static Instant nextChange(ZoneRules one, ZoneRules other, Instant after) {
        ZoneOffsetTransition mine = one.nextTransition(after);
        ZoneOffsetTransition theirs = other.nextTransition(after);
        if (mine == null) {
            return theirs == null ? null : theirs.getInstant();
        }
        return theirs == null || mine.getInstant().isBefore(theirs.getInstant()) ? mine.getInstant() : theirs.getInstant();
    }

    /** The zones of the document ical4j reads itself, each holding an id of its global pool while the document lives. */
    int pooled() {
        return ownZones.size() - agreed.size();
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

    /** A name the document defines unlike the JDK zone of its alias, or one nobody knows, is ical4j's to read (or refuse). */
    @Override
    public ZoneId getZoneId(String tzId) {
        Optional<ZoneId> named = jdkZone(tzId);
        if (named.isPresent()) {
            return named.get();
        }
        if (agreed.containsKey(tzId)) {
            return agreed.get(tzId);
        }
        if (!ownZones.contains(tzId)) {
            named = aliased(tzId);
        }
        return named.orElseGet(() -> inner.getZoneId(tzId));
    }

    @Override
    public String getTzId(String alias) {
        return inner.getTzId(alias);
    }
}
