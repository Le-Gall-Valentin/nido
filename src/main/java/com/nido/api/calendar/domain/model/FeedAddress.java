package com.nido.api.calendar.domain.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** The address of a calendar to follow: http or https — webcal is https by another name —, a host, no credentials. */
public final class FeedAddress {

    static final int MAX_LENGTH = 2000;

    private FeedAddress() {}

    public static URI parse(String raw) {
        String text = raw == null ? "" : raw.strip();
        if (text.isEmpty() || text.length() > MAX_LENGTH) {
            throw new CalendarException.InvalidFeedAddress();
        }
        if (text.regionMatches(true, 0, "webcal://", 0, "webcal://".length())) {
            text = "https://" + text.substring("webcal://".length());
        }
        URI uri;
        try {
            uri = new URI(text);
        } catch (URISyntaxException e) {
            throw new CalendarException.InvalidFeedAddress();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https") || uri.getHost() == null || uri.getRawUserInfo() != null) {
            throw new CalendarException.InvalidFeedAddress();
        }
        return uri.normalize();
    }
}
