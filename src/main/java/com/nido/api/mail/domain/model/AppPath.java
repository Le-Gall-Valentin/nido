package com.nido.api.mail.domain.model;

/**
 * A place in the app a mail links to, as a path: {@code /reset-password#token=…}. A template turns it
 * into a link with {@code ${appUrl + mail.resetPath.value}}; the app's public address is a setting the
 * mail context reads, never taken from a request.
 */
public record AppPath(String value) {

    public AppPath {
        if (value == null || !value.startsWith("/") || value.startsWith("//")) {
            throw new IllegalArgumentException("An app path starts with a single '/': " + value);
        }
    }

    /** Without the fragment: it can carry a live token, and a record holding this path may end up in a log line. */
    @Override
    public String toString() {
        int fragment = value.indexOf('#');
        return fragment < 0 ? value : value.substring(0, fragment) + "#…";
    }
}
