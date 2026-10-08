package com.nido.api.mfa.infrastructure.persistence.scheduler;

import com.nido.api.mfa.domain.port.out.MailCodeStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Clears expired mail codes every hour. Only housekeeping: a code is refused once expired whether or not this has
 * run, and none lives more than ten minutes — not a setting anyone needs to move.
 */
@Component
public class MailCodePurgeScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(MailCodePurgeScheduler.class);
    static final String HOURLY = "0 0 * * * *";

    private final MailCodeStorePort codes;
    private final Clock clock;

    public MailCodePurgeScheduler(MailCodeStorePort codes, Clock clock) {
        this.codes = codes;
        this.clock = clock;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addCronTask(new CronTask(this::purge, HOURLY));
    }

    public void purge() {
        try {
            int purged = codes.deleteExpired(clock.instant());
            // Every hour: an empty purge is not worth a line.
            if (purged > 0) {
                log.info("Purged {} expired two-factor mail codes", purged);
            }
        } catch (Exception e) {
            log.error("Two-factor mail code purge failed — will retry at next scheduled run", e);
        }
    }
}
