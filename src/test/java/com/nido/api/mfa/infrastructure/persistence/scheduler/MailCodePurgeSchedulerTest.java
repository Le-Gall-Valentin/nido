package com.nido.api.mfa.infrastructure.persistence.scheduler;

import com.nido.api.mfa.domain.port.out.MailCodeStorePort;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailCodePurgeSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-10-07T03:00:00Z");

    private final MailCodeStorePort codes = mock(MailCodeStorePort.class);
    private final MailCodePurgeScheduler scheduler = new MailCodePurgeScheduler(codes, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void the_purge_runs_every_hour_on_a_schedule_of_its_own() {
        // Not the refresh tokens' nightly one: a setting about tokens must not move a purge of codes.
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        scheduler.configureTasks(registrar);

        assertThat(registrar.getCronTaskList()).singleElement().extracting(CronTask::getExpression).isEqualTo("0 0 * * * *");
    }

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
