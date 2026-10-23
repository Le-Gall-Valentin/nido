package com.nido.api.calendar.infrastructure.scheduler;

import com.nido.api.calendar.application.port.in.RefreshDueExternalFeedsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Every ten minutes, the followed calendars due — see RefreshDueExternalFeedsHandler. Off in the integration tests. */
@Component
@ConditionalOnProperty(name = "nido.calendar.feed-refresh.enabled", havingValue = "true", matchIfMissing = true)
public class ExternalFeedRefreshScheduler {

    private final RefreshDueExternalFeedsUseCase refreshDue;

    public ExternalFeedRefreshScheduler(RefreshDueExternalFeedsUseCase refreshDue) {
        this.refreshDue = refreshDue;
    }

    @Scheduled(initialDelay = 5, fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void refresh() {
        refreshDue.refreshDue();
    }
}
