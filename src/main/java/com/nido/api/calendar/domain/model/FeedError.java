package com.nido.api.calendar.domain.model;

/** Why a followed calendar could not be read, as stored and shown: "TIMEOUT", "HTTP_404". Never a word of the answer. */
public record FeedError(FeedErrorKind kind, Integer httpStatus) {

    public static FeedError of(FeedErrorKind kind) {
        return new FeedError(kind, null);
    }

    public static FeedError http(int status) {
        return new FeedError(FeedErrorKind.HTTP_STATUS, status);
    }

    public String code() {
        return kind == FeedErrorKind.HTTP_STATUS ? "HTTP_" + httpStatus : kind.name();
    }

    /** A stored code read back; one this version does not know — written by a later one, or altered — reads UNREACHABLE. */
    public static FeedError parse(String code) {
        try {
            return code.startsWith("HTTP_") ? http(Integer.parseInt(code.substring(5))) : of(FeedErrorKind.valueOf(code));
        } catch (IllegalArgumentException unknown) {
            return of(FeedErrorKind.UNREACHABLE);
        }
    }
}
