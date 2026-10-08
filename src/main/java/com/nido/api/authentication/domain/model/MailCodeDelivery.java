package com.nido.api.authentication.domain.model;

/** What came of sending the sign-in code by mail. */
public sealed interface MailCodeDelivery
    permits MailCodeDelivery.Sent, MailCodeDelivery.TooSoon, MailCodeDelivery.LimitReached, MailCodeDelivery.Unavailable {

    record Sent(long resendAfterSeconds) implements MailCodeDelivery {}

    record TooSoon(long retryAfterSeconds) implements MailCodeDelivery {}

    record LimitReached(long retryAfterSeconds) implements MailCodeDelivery {}

    record Unavailable() implements MailCodeDelivery {}
}
