package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarDigestsTest {

    @Test
    void theDigestIsSha256InLowerCaseHex() {
        assertThat(CalendarDigests.sha256Hex("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
