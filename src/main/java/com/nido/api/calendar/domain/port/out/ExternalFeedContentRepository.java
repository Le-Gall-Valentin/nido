package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.FeedToShow;

import java.util.List;
import java.util.UUID;

/** The calendars a space follows, as a calendar read shows them. */
public interface ExternalFeedContentRepository {

    /** What a calendar read needs of each calendar the space follows: nothing sealed is opened for it. */
    List<FeedToShow> findShownBySpaceId(UUID spaceId);

    /** The file downloaded last, read only when the reader does not hold it in memory already. */
    String loadContent(UUID feedId);
}
