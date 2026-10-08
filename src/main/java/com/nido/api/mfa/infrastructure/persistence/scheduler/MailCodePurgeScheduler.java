package com.nido.api.mfa.infrastructure.persistence.scheduler;

import com.nido.api.infrastructure.config.NidoProperties;
import com.nido.api.mfa.domain.port.out.MailCodeStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Clears expired mail codes, on the refresh-token purge's schedule (NIDO_REFRESH_TOKEN_PURGE_CRON). Only
 * housekeeping: a code is refused once expired whether or not this has run.
 */
@Component
public class MailCodePurgeScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(MailCodePurgeScheduler.class);

    private final MailCodeStorePort codes;
    private final Clock clock;
    private final String cron;

    @Autowired
    public MailCodePurgeScheduler(MailCodeStorePort codes, Clock clock, NidoProperties properties) {
        this(codes, clock, properties.refreshToken().purgeCron());
    }

    MailCodePurgeScheduler(MailCodeStorePort codes, Clock clock, String cron) {
        this.codes = codes;
        this.clock = clock;
        this.cron = cron;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addCronTask(new CronTask(this::purge, cron));
    }

    public void purge() {
        try {
            log.info("Purged {} expired two-factor mail codes", codes.deleteExpired(clock.instant()));
        } catch (Exception e) {
            log.error("Two-factor mail code purge failed — will retry at next scheduled run", e);
        }
    }
}
