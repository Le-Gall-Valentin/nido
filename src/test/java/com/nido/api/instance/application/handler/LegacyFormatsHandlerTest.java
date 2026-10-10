package com.nido.api.instance.application.handler;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyFormatsHandlerTest {

    private final InstanceFakes.MemoryState state = new InstanceFakes.MemoryState();
    private final LegacyFormatsHandler handler = new LegacyFormatsHandler(state, Clock.systemUTC());

    @Test
    void earlier_formats_are_open_until_closed_and_closing_twice_changes_nothing() {
        assertThat(handler.closed()).isFalse();

        handler.closeForGood();
        handler.closeForGood();

        assertThat(handler.closed()).isTrue();
        assertThat(state.legacyFormatsClosed).isTrue();
    }
}
