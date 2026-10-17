package com.nido.api.calendar.domain.model;

/** Why a followed calendar could not be read. */
public enum FeedErrorKind {
    INVALID_URL, BLOCKED_ADDRESS, UNREACHABLE, TIMEOUT, HTTP_STATUS, TOO_MANY_REDIRECTS, TOO_LARGE, NOT_ICS
}
