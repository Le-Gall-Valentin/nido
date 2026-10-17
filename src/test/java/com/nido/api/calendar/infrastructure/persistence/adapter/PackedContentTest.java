package com.nido.api.calendar.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PackedContentTest {

    @Test
    void aCalendarIsPackedSmallerAndUnpackedWhole() {
        String ics = "BEGIN:VCALENDAR\r\n" + "BEGIN:VEVENT\r\nSUMMARY:Réunion\r\nEND:VEVENT\r\n".repeat(1000) + "END:VCALENDAR\r\n";
        String packed = PackedContent.pack(ics);
        assertThat(packed.length()).isLessThan(ics.length() / 10);
        assertThat(PackedContent.unpack(packed)).isEqualTo(ics);
    }
}
