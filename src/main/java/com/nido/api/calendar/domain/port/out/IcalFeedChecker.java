package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.FeedCheck;

/** Whether a downloaded file is a calendar nido can read, the name it gives itself (X-WR-CALNAME), and what it weighs. */
public interface IcalFeedChecker {
    /** @throws com.nido.api.calendar.domain.model.CalendarException.NotAnIcalFile when it is not */
    FeedCheck check(String content);
}
