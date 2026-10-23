package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.domain.model.CalendarException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IcalUploadTest {

    private static final int LIMIT = IcalUpload.MAX_MEGABYTES * 1024 * 1024;

    private static byte[] bytes(int length) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) 'x');
        return bytes;
    }

    @Test
    void aFileOfExactlyTheLimitIsRead() throws Exception {
        assertThat(IcalUpload.read(new ByteArrayInputStream(bytes(LIMIT)))).hasSize(LIMIT);
    }

    @Test
    void oneByteMoreIsRefusedAndSaysTheLimit() {
        assertThatThrownBy(() -> IcalUpload.read(new ByteArrayInputStream(bytes(LIMIT + 1))))
            .isInstanceOfSatisfying(CalendarException.ImportFileTooLarge.class,
                tooLarge -> assertThat(tooLarge.maximumMegabytes()).isEqualTo(5));
    }

    @Test
    void aByteOrderMarkIsDroppedAndNothingElse() throws Exception {
        byte[] marked = "﻿BEGIN:VCALENDAR﻿".getBytes(StandardCharsets.UTF_8);

        assertThat(IcalUpload.read(new ByteArrayInputStream(marked))).isEqualTo("BEGIN:VCALENDAR﻿");
    }

    @Test
    void anEmptyBodyIsAnEmptyText() throws Exception {
        assertThat(IcalUpload.read(new ByteArrayInputStream(new byte[0]))).isEmpty();
    }
}
