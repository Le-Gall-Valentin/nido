package com.nido.api.mail.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailModelTest {

    record SampleMail(String username) implements MailContent {
        @Override public String template() { return "test/sample"; }
    }

    @Test
    void a_recipient_needs_an_address() {
        assertThatThrownBy(() -> new Recipient(" ", "Jane")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Recipient(null, "Jane")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Recipient("jane@example.com", null).displayName()).isNull();
    }

    @Test
    void an_app_path_starts_with_a_slash() {
        assertThatThrownBy(() -> new AppPath("reset-password")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AppPath("https://evil.example/")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new AppPath("/reset-password#token=abc").value()).isEqualTo("/reset-password#token=abc");
    }

    @Test
    void a_request_without_expiry_never_expires() {
        MailRequest request = MailRequest.of(new Recipient("jane@example.com", "Jane"), Locale.FRENCH, new SampleMail("jane"));

        assertThat(request.expiresAt()).isNull();
        assertThatThrownBy(() -> new MailRequest(null, Locale.FRENCH, new SampleMail("jane"), null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void an_entry_expires_at_its_instant_not_after_it() {
        Instant expiresAt = Instant.parse("2026-09-28T10:30:00Z");
        OutboxEntry entry = new OutboxEntry(UUID.randomUUID(), "test/sample",
            new OutgoingMail(new Recipient("jane@example.com", null), new RenderedMail("s", "<p>h</p>", "t")), 0, expiresAt);

        assertThat(entry.isExpiredAt(expiresAt.minusSeconds(1))).isFalse();
        assertThat(entry.isExpiredAt(expiresAt)).isTrue();
        assertThat(new OutboxEntry(entry.id(), entry.kind(), entry.mail(), 0, null).isExpiredAt(expiresAt)).isFalse();
    }
}
