package com.nido.api.calendar.infrastructure.ical;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentContainer;
import net.fortuna.ical4j.model.Property;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * What a calendar kept in memory weighs, about what it holds of the heap. The text of a file is not what it holds:
 * measured, a realistic calendar of 5 MB retains 39 MB, one made of lines of {@code X-A:1} 142 MB, one property with a
 * million {@code ;A=1} parameters 143 MB. So each property and each parameter ical4j made counts too, each change its own
 * zones list (24 MB for one listing 60 000), and each id of ical4j's zone pool they hold while the calendar is kept.
 */
final class FeedWeight {

    /** A property or a parameter: brings the realistic calendar to its weight, the others above theirs. */
    static final int PROPERTY_WEIGHT = 256;
    /** A change a zone of the file lists, as ical4j holds it. */
    static final int ZONE_CHANGE_WEIGHT = 400;
    /**
     * A zone ical4j reads itself holds an id of its global pool of 1 500, for every document of the server, for as long as
     * its calendar is kept: at this weight the whole cache holds a thousand at most, and leaves the other ids to the
     * documents being read. Spent, the pool would refuse every Outlook file of every space.
     */
    static final long POOLED_ZONE_WEIGHT = ParsedFeeds.CACHE_WEIGHT / 1_000;

    private FeedWeight() {}

    static int of(String text, IcalSupport.Read read) {
        Calendar calendar = read.calendar();
        long parts = parts(calendar.getProperties());
        Deque<Component> pending = new ArrayDeque<>(calendar.getComponents());
        while (!pending.isEmpty()) {
            Component component = pending.pop();
            parts += parts(component.getProperties());
            if (component instanceof ComponentContainer<?> container) {
                pending.addAll(container.getComponents());
            }
        }
        long weight = text.length() + PROPERTY_WEIGHT * parts + ZONE_CHANGE_WEIGHT * read.zoneChanges()
            + POOLED_ZONE_WEIGHT * read.pooledZones();
        return (int) Math.min(Integer.MAX_VALUE, weight);
    }

    private static long parts(List<Property> properties) {
        long parts = properties.size();
        for (Property property : properties) {
            parts += property.getParameterList().getAll().size();
        }
        return parts;
    }
}
