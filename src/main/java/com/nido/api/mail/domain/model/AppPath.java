package com.nido.api.mail.domain.model;

/**
 * A place in the app a mail links to, as a path: {@code /reset-password#token=…}. A template turns it
 * into a link with {@code ${appUrl + mail.resetPath.value}}; the app's public address (NIDO_APP_URL)
 * is known to the mail context alone, and is never taken from a request.
 */
public record AppPath(String value) {

    public AppPath {
        if (value == null || !value.startsWith("/") || value.startsWith("//")) {
            throw new IllegalArgumentException("An app path starts with a single '/': " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
