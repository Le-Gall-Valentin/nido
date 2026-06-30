package com.synapsys.api.authentication.infrastructure.persistence.scheduler;

import com.synapsys.api.authentication.domain.port.out.RefreshTokenConfigPort;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenMaintenancePort;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenSchedulePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class RefreshTokenPurgeScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenPurgeScheduler.class);

    private final RefreshTokenMaintenancePort refreshTokenMaintenance;
    private final int refreshTokenExpiryDays;
    private final String purgeCron;

    public RefreshTokenPurgeScheduler(RefreshTokenMaintenancePort refreshTokenMaintenance,
                                      RefreshTokenConfigPort tokenConfig,
                                      RefreshTokenSchedulePort schedulePort) {
        this.refreshTokenMaintenance = refreshTokenMaintenance;
        this.refreshTokenExpiryDays = tokenConfig.refreshTokenExpiryDays();
        this.purgeCron = schedulePort.refreshTokenPurgeCron();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addCronTask(new CronTask(this::purgeExpiredTokens, purgeCron));
    }

    public void purgeExpiredTokens() {
        try {
            Instant now = Instant.now();
            Instant cutoff = now.minus(refreshTokenExpiryDays, ChronoUnit.DAYS);
            int deleted = refreshTokenMaintenance.deleteExpiredAndRevoked(now, cutoff);
            log.info("Purged {} expired/revoked refresh tokens", deleted);
        } catch (Exception e) {
            log.error("Refresh token purge failed — will retry at next scheduled run", e);
        }
    }
}