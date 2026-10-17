package com.nido.api.calendar.infrastructure.ical;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
}
