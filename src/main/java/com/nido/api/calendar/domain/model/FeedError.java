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

    public static FeedError parse(String code) {
        return code.startsWith("HTTP_") ? http(Integer.parseInt(code.substring(5))) : of(FeedErrorKind.valueOf(code));
    }
}
