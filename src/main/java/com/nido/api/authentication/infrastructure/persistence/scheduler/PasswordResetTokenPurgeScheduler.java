package com.nido.api.authentication.infrastructure.persistence.scheduler;

import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.RefreshTokenSchedulePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Clears expired reset links, on the refresh-token purge's schedule (NIDO_REFRESH_TOKEN_PURGE_CRON). */
@Component
public class PasswordResetTokenPurgeScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenPurgeScheduler.class);

    private final PasswordResetTokenRepository tokens;
    private final Clock clock;
    private final String cron;

    public PasswordResetTokenPurgeScheduler(PasswordResetTokenRepository tokens, Clock clock, RefreshTokenSchedulePort schedule) {
        this.tokens = tokens;
        this.clock = clock;
        this.cron = schedule.refreshTokenPurgeCron();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addCronTask(new CronTask(this::purge, cron));
    }

    public void purge() {
        try {
            log.info("Purged {} expired password reset tokens", tokens.deleteExpired(clock.instant()));
        } catch (Exception e) {
            log.error("Password reset token purge failed — will retry at next scheduled run", e);
        }
    }
}
