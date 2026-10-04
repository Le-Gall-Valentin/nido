package com.nido.api.notifications.domain.model;

public abstract sealed class NotificationException extends RuntimeException
    permits NotificationException.UnknownChannel, NotificationException.UnknownType {

    private NotificationException(String message) {
        super(message);
    }

    /** Not a channel of this installation: unknown to the code, or not configured here (mail without SMTP). */
    public static final class UnknownChannel extends NotificationException {
        public UnknownChannel() {
            super("Unknown notification channel");
        }
    }

    public static final class UnknownType extends NotificationException {
        public UnknownType() {
            super("Unknown notification type");
        }
    }
}
