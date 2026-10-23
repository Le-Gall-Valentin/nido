package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.application.port.in.FollowExternalFeedUseCase;
import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedAddress;
import com.nido.api.calendar.domain.model.FeedDownload;
import com.nido.api.calendar.domain.model.FeedCheck;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedErrorKind;
import com.nido.api.calendar.domain.model.FollowExternalFeedCommand;
import com.nido.api.calendar.domain.port.out.ExternalFeedDownloader;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.calendar.domain.port.out.IcalFeedChecker;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.net.URI;
import java.time.Clock;

/**
 * Follows a calendar for a space — downloaded at once, and refused if that fails or is no calendar: a calendar kept
 * broken from its first day would only ever show an error. Not in a transaction: see ExternalFeedRefresher.
 */
@ApplicationService
public class FollowExternalFeedHandler implements FollowExternalFeedUseCase {

    static final int MAX_PER_SPACE = 10;
    static final int MAX_NAME = 100;

    private final ExternalFeedRepository feeds;
    private final ExternalFeedDownloader downloader;
    private final IcalFeedChecker checker;
    private final Clock clock;

    public FollowExternalFeedHandler(ExternalFeedRepository feeds, ExternalFeedDownloader downloader,
                                     IcalFeedChecker checker, Clock clock) {
        this.feeds = feeds;
        this.downloader = downloader;
        this.checker = checker;
        this.clock = clock;
    }

    @Override
    public ExternalFeed follow(FollowExternalFeedCommand command, SpaceMembership caller) {
        caller.ensureCanWrite();
        URI url = FeedAddress.parse(command.url());
        // Said before a download of up to fifteen seconds; decided when the calendar is written.
        if (feeds.countBySpaceId(caller.spaceId()) >= MAX_PER_SPACE) {
            throw new CalendarException.TooManyExternalFeeds(MAX_PER_SPACE);
        }
        FeedDownload download = downloader.download(url, null, null);
        if (!(download instanceof FeedDownload.Downloaded downloaded)) {
            throw new CalendarException.FeedUnavailable(download instanceof FeedDownload.Failed failed
                ? failed.error() : FeedError.of(FeedErrorKind.UNREACHABLE));
        }
        String ownName;
        try {
            FeedCheck check = checker.check(downloaded.content());
            if (check.tooHeavy()) {
                throw new CalendarException.FeedUnavailable(FeedError.of(FeedErrorKind.TOO_LARGE));
            }
            ownName = check.name().map(String::strip).filter(n -> !n.isEmpty()).orElse(null);
        } catch (CalendarException.NotAnIcalFile notACalendar) {
            throw new CalendarException.FeedUnavailable(FeedError.of(FeedErrorKind.NOT_ICS));
        }
        String name = command.name() != null && !command.name().isBlank() ? command.name().strip()
            : ownName != null ? ownName : url.getHost();
        // Counted again as it is written: another follow may have passed the limit during the download.
        return feeds.createUnlessFollowing(MAX_PER_SPACE, caller.spaceId(), truncated(name), url,
                ExternalFeed.colorOrDefault(command.color()), caller.userId(), downloaded.content(), downloaded.etag(),
                downloaded.lastModified(), clock.instant())
            .orElseThrow(() -> new CalendarException.TooManyExternalFeeds(MAX_PER_SPACE));
    }

    /** At most {@link #MAX_NAME} characters, never cutting a character in two. */
    private static String truncated(String name) {
        return name.codePointCount(0, name.length()) <= MAX_NAME ? name
            : name.substring(0, name.offsetByCodePoints(0, MAX_NAME));
    }
}
