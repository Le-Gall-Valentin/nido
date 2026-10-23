package com.nido.api.calendar.domain.model;

/** {@code name} blank: the name the calendar gives itself, or its host. */
public record FollowExternalFeedCommand(String url, String name, String color) {

    /** Never the address: it often holds a secret. */
    @Override
    public String toString() {
        return "FollowExternalFeedCommand[name=" + name + ", color=" + color + "]";
    }
}
