package com.nido.api.authentication.infrastructure.persistence.scheduler;

import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.RefreshTokenSchedulePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenPurgeSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-09-30T03:00:00Z");
    private static final RefreshTokenSchedulePort REFRESH_TOKEN_CRON = () -> "0 0 3 * * *";

    @Mock PasswordResetTokenRepository tokens;

    private PasswordResetTokenPurgeScheduler scheduler() {
        return new PasswordResetTokenPurgeScheduler(tokens, Clock.fixed(NOW, ZoneOffset.UTC), REFRESH_TOKEN_CRON);
    }

    @Test
    void removes_the_links_expired_by_now() {
        when(tokens.deleteExpired(NOW)).thenReturn(3);

        scheduler().purge();

        verify(tokens).deleteExpired(NOW);
    }

    @Test
    void runs_on_the_refresh_token_purge_schedule() {
        ScheduledTaskRegistrar registrar = mock(ScheduledTaskRegistrar.class);
        ArgumentCaptor<CronTask> task = ArgumentCaptor.forClass(CronTask.class);

        scheduler().configureTasks(registrar);

        verify(registrar).addCronTask(task.capture());
        assertThat(task.getValue().getExpression()).isEqualTo("0 0 3 * * *");
    }

    @Test
    void a_failed_purge_waits_for_the_next_run_instead_of_breaking_the_scheduler() {
        when(tokens.deleteExpired(NOW)).thenThrow(new IllegalStateException("database down"));

        assertThatCode(() -> scheduler().purge()).doesNotThrowAnyException();
    }
}
