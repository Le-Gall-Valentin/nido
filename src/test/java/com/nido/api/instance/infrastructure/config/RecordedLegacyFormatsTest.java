package com.nido.api.instance.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RecordedLegacyFormatsTest {

    private final AtomicBoolean recorded = new AtomicBoolean();
    private final AtomicInteger asked = new AtomicInteger();
    private final AtomicInteger closings = new AtomicInteger();
    private final RecordedLegacyFormats formats = new RecordedLegacyFormats(() -> {
        asked.incrementAndGet();
        return recorded.get();
    }, closings::incrementAndGet);

    @Test
    void the_record_is_asked_while_it_says_open_and_never_again_once_it_says_closed() {
        assertThat(formats.closed()).isFalse();
        assertThat(formats.closed()).isFalse();

        // Closed by another instance's start in between.
        recorded.set(true);

        assertThat(formats.closed()).isTrue();
        assertThat(formats.closed()).isTrue();
        assertThat(asked).hasValue(3);
    }

    @Test
    void closing_for_good_records_it_and_answers_closed_from_then_on_without_asking() {
        formats.closeForGood();

        assertThat(closings).hasValue(1);
        assertThat(formats.closed()).isTrue();
        assertThat(asked).hasValue(0);
    }
}
