package com.nido.api.calendar.domain.port.out;

import com.nido.api.calendar.domain.model.FeedDownload;

import java.net.URI;

/** Downloads a followed calendar — never from an address of this machine or, unless allowed, of its network. */
public interface ExternalFeedDownloader {
    /** {@code etag} and {@code lastModified}, when known, ask for nothing if nothing changed. */
    FeedDownload download(URI url, String etag, String lastModified);
}
