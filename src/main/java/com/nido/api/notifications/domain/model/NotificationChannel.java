package com.nido.api.notifications.domain.model;

import java.util.Arrays;
import java.util.Optional;

/** A way a notification reaches someone. Stored and exchanged as its code. */
public enum NotificationChannel {
    EMAIL("email");

    private final String code;

    NotificationChannel(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<NotificationChannel> fromCode(String code) {
        return Arrays.stream(values()).filter(channel -> channel.code.equals(code)).findFirst();
    }
}
