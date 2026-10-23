package com.nido.api.calendar.domain.model;

/** What asking a followed calendar's address gave. */
public sealed interface FeedDownload {

    record Downloaded(String content, String etag, String lastModified) implements FeedDownload {}

    /** The calendar has not changed since the version kept (304). */
    record NotModified() implements FeedDownload {}

    record Failed(FeedError error) implements FeedDownload {}
}
