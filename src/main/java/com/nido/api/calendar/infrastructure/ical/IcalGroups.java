package com.nido.api.calendar.infrastructure.ical;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** The events of a file grouped by UID — a rule and the occurrences it edited on their own — as the import and the followed calendars both read them. */
final class IcalGroups {

    private static final String ANONYMOUS = "\0";

    private IcalGroups() {}

    /** By UID, stripped (a blank one is none). An event with no UID stands alone: under a key no UID can be. */
    static Map<String, List<VEvent>> byUid(Iterable<VEvent> events) {
        Map<String, List<VEvent>> byUid = new LinkedHashMap<>();
        int anonymous = 0;
        for (VEvent event : events) {
            String uid = event.getProperty(Property.UID).map(Property::getValue).map(String::strip).filter(s -> !s.isEmpty()).orElse(null);
            byUid.computeIfAbsent(uid != null ? uid : ANONYMOUS + anonymous++, key -> new ArrayList<>()).add(event);
        }
        return byUid;
    }

    /** The UID a group's key stands for; null for an event that had none. */
    static String uidOf(String key) {
        return key.startsWith(ANONYMOUS) ? null : key;
    }

    static List<VEvent> masters(List<VEvent> group) {
        return group.stream().filter(e -> e.getProperty(Property.RECURRENCE_ID).isEmpty()).toList();
    }

    static List<VEvent> overrides(List<VEvent> group) {
        return group.stream().filter(e -> e.getProperty(Property.RECURRENCE_ID).isPresent()).toList();
    }

    /**
     * One edited occurrence per slot. A file may hold the same RECURRENCE-ID twice — duplicated blocks, merged exports,
     * versions that differ by SEQUENCE —, and nido holds one event per slot: the highest SEQUENCE stands, else the first.
     * {@code dropped} is handed the others, in the file's order.
     */
    static List<VEvent> onePerSlot(List<VEvent> overrides, ZoneId zone, Consumer<VEvent> dropped) {
        // By position, not by equality: two copies of one block are equal components, and only one of them stands.
        Map<LocalDateTime, Integer> standing = new HashMap<>();
        boolean[] out = new boolean[overrides.size()];
        for (int i = 0; i < overrides.size(); i++) {
            Optional<LocalDateTime> slot = RecurringImport.replaced(overrides.get(i), zone);
            if (slot.isEmpty()) {
                continue;
            }
            Integer before = standing.putIfAbsent(slot.get(), i);
            if (before != null) {
                boolean newer = sequenceOf(overrides.get(i)) > sequenceOf(overrides.get(before));
                out[newer ? before : i] = true;
                if (newer) {
                    standing.put(slot.get(), i);
                }
            }
        }
        List<VEvent> kept = new ArrayList<>();
        for (int i = 0; i < overrides.size(); i++) {
            if (out[i]) {
                dropped.accept(overrides.get(i));
            } else {
                kept.add(overrides.get(i));
            }
        }
        return kept;
    }

    private static int sequenceOf(VEvent event) {
        return event.getProperty(Property.SEQUENCE).map(Property::getValue).map(String::strip).map(value -> {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException unreadable) {
                return 0;
            }
        }).orElse(0);
    }
}
