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

    @Test
    void lettersNoAccentCanBeTakenOffAreSpelledOut() {
        LocalDate day = LocalDate.of(2026, 10, 13);
        assertThat(IcalFiles.filename("Straße", day)).isEqualTo("nido-strasse-2026-10-13.ics");
        assertThat(IcalFiles.filename("Œuvres d'Ærø", day)).isEqualTo("nido-oeuvres-d-aero-2026-10-13.ics");
        assertThat(IcalFiles.filename("Østergård", day)).isEqualTo("nido-ostergard-2026-10-13.ics");
    }

    @Test
    void aLongNameIsCutAtFortyCharactersWithoutEndingOnADash() {
        LocalDate day = LocalDate.of(2026, 10, 13);
        assertThat(IcalFiles.filename("a".repeat(60), day)).isEqualTo("nido-" + "a".repeat(40) + "-2026-10-13.ics");
        // The 40th character is a dash: the cut ends on the letter before it.
        assertThat(IcalFiles.filename("b".repeat(39) + " de la famille", day)).isEqualTo("nido-" + "b".repeat(39) + "-2026-10-13.ics");
    }
}
