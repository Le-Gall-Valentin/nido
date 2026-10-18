package com.nido.api.calendar.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackedContentTest {

    @Test
    void aCalendarIsPackedSmallerAndUnpackedWhole() {
        String ics = "BEGIN:VCALENDAR\r\n" + "BEGIN:VEVENT\r\nSUMMARY:Réunion\r\nEND:VEVENT\r\n".repeat(1000) + "END:VCALENDAR\r\n";
        String packed = PackedContent.pack(ics);
        assertThat(packed.length()).isLessThan(ics.length() / 10);
        assertThat(PackedContent.unpack(packed)).isEqualTo(ics);
    }

    @Test
    void aStoredValueThatIsNotAPackedCalendarSaysSo() {
        assertThatThrownBy(() -> PackedContent.unpack("not base64 at all!"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("not a packed calendar");
        assertThatThrownBy(() -> PackedContent.unpack("aGVsbG8="))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("not a packed calendar");
    }
}
