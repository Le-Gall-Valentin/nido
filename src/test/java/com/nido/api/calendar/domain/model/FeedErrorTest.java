package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedErrorTest {

    @Test
    void anHttpStatusIsNamedWithItsCode() {
        assertThat(FeedError.http(404).code()).isEqualTo("HTTP_404");
        assertThat(FeedError.parse("HTTP_404")).isEqualTo(FeedError.http(404));
    }

    @Test
    void anyOtherErrorIsNamedByItsKind() {
        assertThat(FeedError.of(FeedErrorKind.TIMEOUT).code()).isEqualTo("TIMEOUT");
        assertThat(FeedError.parse("BLOCKED_ADDRESS")).isEqualTo(FeedError.of(FeedErrorKind.BLOCKED_ADDRESS));
    }

    @Test
    void aCodeThisVersionDoesNotKnowIsReadAsUnreachable() {
        // Written by a later version, or altered: one feed's last error must not fail the list of the space.
        assertThat(FeedError.parse("SOMETHING_NEW")).isEqualTo(FeedError.of(FeedErrorKind.UNREACHABLE));
        assertThat(FeedError.parse("HTTP_abc")).isEqualTo(FeedError.of(FeedErrorKind.UNREACHABLE));
        assertThat(FeedError.parse("")).isEqualTo(FeedError.of(FeedErrorKind.UNREACHABLE));
    }
}
