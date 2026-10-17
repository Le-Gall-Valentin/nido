package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Ical4jFeedReaderAdapterTest {

    private final Ical4jFeedReaderAdapter reader = new Ical4jFeedReaderAdapter();

    @Test
    void aCalendarGivesTheNameItCallsItself() {
        assertThat(reader.calendarName("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\nX-WR-CALNAME:Vacances scolaires\r\nEND:VCALENDAR\r\n"))
            .contains("Vacances scolaires");
        assertThat(reader.calendarName("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\nEND:VCALENDAR\r\n")).isEmpty();
    }

    @Test
    void anythingElseIsNotACalendar() {
        assertThatThrownBy(() -> reader.calendarName("<html>Moved</html>")).isInstanceOf(CalendarException.NotAnIcalFile.class);
    }
}
