package com.nido.api.calendar.domain.port.out;

import java.util.Optional;

/** Whether a downloaded file is a calendar nido can read, and the name it gives itself (X-WR-CALNAME). */
public interface IcalFeedChecker {
    /** @throws com.nido.api.calendar.domain.model.CalendarException.NotAnIcalFile when it is not */
    Optional<String> calendarName(String content);
}
