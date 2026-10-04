package com.nido.api.notifications.domain.model;

/**
 * Something that happened to an account, as the context it happened in writes it: a public record,
 * annotated with {@link NotificationKind}, that also implements the content type of every channel it can
 * travel on — {@code MailContent} today. It lives in the sending context's infrastructure, beside its
 * templates. This context never learns what a notification says, only its kind.
 */
public interface Notification {
}
