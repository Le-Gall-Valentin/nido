package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.port.out.IcalFeedChecker;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Reads the calendars a space follows: whether a download is one, and — Task 22 — their occurrences on a window. */
@Component
public class Ical4jFeedReaderAdapter implements IcalFeedChecker {

    @Override
    public Optional<String> calendarName(String content) {
        Calendar calendar = IcalSupport.parse(content);
        return calendar.getProperty("X-WR-CALNAME").map(Property::getValue).map(String::strip).filter(name -> !name.isEmpty());
    }
}
