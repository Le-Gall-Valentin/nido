package com.nido.api.mfa.infrastructure.persistence.scheduler;

import com.nido.api.mfa.domain.port.out.MailCodeStorePort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailCodePurgeSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-10-07T03:00:00Z");

    private final MailCodeStorePort codes = mock(MailCodeStorePort.class);
    private final MailCodePurgeScheduler scheduler =
        new MailCodePurgeScheduler(codes, Clock.fixed(NOW, ZoneOffset.UTC), "0 0 3 * * *");

    @Test
    void the_purge_clears_what_has_expired_by_now() {
        scheduler.purge();

        verify(codes).deleteExpired(NOW);
    }

    @Test
    void a_failed_purge_waits_for_the_next_run_instead_of_breaking_the_scheduler() {
        when(codes.deleteExpired(NOW)).thenThrow(new IllegalStateException("database down"));

        assertThatCode(scheduler::purge).doesNotThrowAnyException();
    }
}
