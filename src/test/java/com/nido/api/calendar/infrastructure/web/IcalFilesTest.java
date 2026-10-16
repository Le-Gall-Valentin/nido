package com.nido.api.calendar.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IcalFilesTest {

    @Test
    void theFileIsNamedAfterTheCalendarInPlainLettersAndTheDay() {
        assertThat(IcalFiles.filename("Chez Valentin & Léa", LocalDate.of(2026, 10, 13))).isEqualTo("nido-chez-valentin-lea-2026-10-13.ics");
    }

    @Test
    void aNameWithNoLetterLeftGivesAPlainFileName() {
        assertThat(IcalFiles.filename("🏡", LocalDate.of(2026, 10, 13))).isEqualTo("nido-2026-10-13.ics");
    }
}
