package com.nido.api.mail.infrastructure.scheduler;

import com.nido.api.infrastructure.config.ConditionalOnMailEnabled;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * The safety net behind the after-commit trigger: every 30 seconds, whatever is due — retries whose
 * time has come, and mails a crash or a restart left behind.
 */
@Component
@ConditionalOnMailEnabled
public class MailDispatchScheduler {

    private final AfterCommitDispatchTrigger trigger;

    public MailDispatchScheduler(AfterCommitDispatchTrigger trigger) {
        this.trigger = trigger;
    }

    @Scheduled(initialDelay = 30, fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void sweep() {
        trigger.wakeUp();
    }
}
