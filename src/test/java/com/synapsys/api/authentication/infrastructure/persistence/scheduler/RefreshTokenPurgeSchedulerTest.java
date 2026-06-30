package com.synapsys.api.authentication.infrastructure.persistence.scheduler;

import com.synapsys.api.authentication.domain.port.out.RefreshTokenConfigPort;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenMaintenancePort;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenSchedulePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenPurgeSchedulerTest {

    @Mock
    RefreshTokenMaintenancePort refreshTokenRepository;

    private static final int EXPIRY_DAYS = 30;
    private static final String PURGE_CRON = "0 0 3 * * *";

    private RefreshTokenPurgeScheduler scheduler() {
        RefreshTokenConfigPort tokenConfig = () -> EXPIRY_DAYS;
        RefreshTokenSchedulePort schedulePort = () -> PURGE_CRON;
        return new RefreshTokenPurgeScheduler(refreshTokenRepository, tokenConfig, schedulePort);
    }

    @Test
    void purgeExpiredTokens_delegatesToRepository() {
        when(refreshTokenRepository.deleteExpiredAndRevoked(any(Instant.class), any(Instant.class))).thenReturn(5);

        scheduler().purgeExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredAndRevoked(any(Instant.class), any(Instant.class));
    }

    @Test
    void configureTasks_registersCronTaskUsingCronFromConfigPort() {
        var customCron = "0 0 4 * * *";
        RefreshTokenConfigPort tokenConfig = () -> EXPIRY_DAYS;
        RefreshTokenSchedulePort schedulePort = () -> customCron;
        var scheduler = new RefreshTokenPurgeScheduler(refreshTokenRepository, tokenConfig, schedulePort);
        var registrar = mock(ScheduledTaskRegistrar.class);
        var captor = ArgumentCaptor.forClass(CronTask.class);

        scheduler.configureTasks(registrar);

        verify(registrar).addCronTask(captor.capture());
        assertThat(captor.getValue().getExpression()).isEqualTo(customCron);
    }

    @Test
    void purgeExpiredTokens_cutoffMatchesRefreshTokenExpiryDays() {
        when(refreshTokenRepository.deleteExpiredAndRevoked(any(Instant.class), any(Instant.class))).thenReturn(0);
        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);

        scheduler().purgeExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredAndRevoked(nowCaptor.capture(), cutoffCaptor.capture());
        long daysBetween = Duration.between(cutoffCaptor.getValue(), nowCaptor.getValue()).toDays();
        assertThat(daysBetween).isBetween((long) EXPIRY_DAYS - 1, (long) EXPIRY_DAYS + 1);
    }
}