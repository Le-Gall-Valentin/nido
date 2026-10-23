package com.nido.api.calendar.application.service;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedDownload;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedErrorKind;
import com.nido.api.calendar.domain.port.out.ExternalFeedDownloader;
import com.nido.api.calendar.domain.port.out.ExternalFeedRefreshRepository;
import com.nido.api.calendar.domain.port.out.IcalFeedChecker;
import com.nido.api.shared.annotation.ApplicationService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Asks a followed calendar's address again — "has it changed?" when it said how to ask — and keeps what it answers. A
 * failure, of the address or of the file, keeps the calendar it had: the space goes on seeing it, the list says why.
 * Not in a transaction: the download may take fifteen seconds, and each write is one of its own.
 */
@ApplicationService
public class ExternalFeedRefresher {

    /** A calendar is asked at most once a minute: what a member can ask, and what never doubles a scheduled asking. */
    static final Duration AT_MOST_EVERY = Duration.ofMinutes(1);

    private final ExternalFeedRefreshRepository feeds;
    private final ExternalFeedDownloader downloader;
    private final IcalFeedChecker checker;
    private final Clock clock;

    public ExternalFeedRefresher(ExternalFeedRefreshRepository feeds, ExternalFeedDownloader downloader, IcalFeedChecker checker,
                                 Clock clock) {
        this.feeds = feeds;
        this.downloader = downloader;
        this.checker = checker;
        this.clock = clock;
    }

    /** Refuses ({@code RefreshTooSoon}) a calendar asked less than a minute ago, by a member or on its own. */
    public ExternalFeed refresh(ExternalFeed feed) {
        Instant now = clock.instant();
        // Claimed before asking, in one statement: of two askings within the minute, the second is refused.
        if (!feeds.claimAttempt(feed.id(), now, now.minus(AT_MOST_EVERY))) {
            throw new CalendarException.RefreshTooSoon();
        }
        switch (downloader.download(feed.url(), feed.etag(), feed.lastModified())) {
            case FeedDownload.Downloaded downloaded -> {
                try {
                    if (checker.check(downloaded.content()).tooHeavy()) {
                        // Too much to keep in memory: the last good one stays.
                        feeds.recordFailure(feed.id(), FeedError.of(FeedErrorKind.TOO_LARGE), now);
                    } else {
                        feeds.recordSuccess(feed.id(), downloaded.content(), downloaded.etag(), downloaded.lastModified(), now);
                    }
                } catch (CalendarException.NotAnIcalFile notACalendar) {
                    feeds.recordFailure(feed.id(), FeedError.of(FeedErrorKind.NOT_ICS), now);
                }
            }
            case FeedDownload.NotModified ignored -> feeds.recordNotModified(feed.id(), now);
            case FeedDownload.Failed failed -> feeds.recordFailure(feed.id(), failed.error(), now);
        }
        return feeds.findById(feed.id()).orElseThrow(CalendarException.ExternalFeedNotFound::new);
    }
}
