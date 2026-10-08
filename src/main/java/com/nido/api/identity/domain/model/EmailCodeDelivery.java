package com.nido.api.identity.domain.model;

/** What came of asking for a code at the new address. A refusal is an answer, worded by whoever asked. */
public sealed interface EmailCodeDelivery
    permits EmailCodeDelivery.Sent, EmailCodeDelivery.TooSoon, EmailCodeDelivery.LimitReached, EmailCodeDelivery.Unavailable {

    /** On its way; another can be asked for after {@code resendAfterSeconds}. */
    record Sent(long resendAfterSeconds) implements EmailCodeDelivery {}

    /** One left for the same address moments ago — it still works. */
    record TooSoon(long retryAfterSeconds) implements EmailCodeDelivery {}

    /** The account has had all the mails of code its window allows. */
    record LimitReached(long retryAfterSeconds) implements EmailCodeDelivery {}

    /** Mail is off: nothing can leave, so nothing can prove the address. */
    record Unavailable() implements EmailCodeDelivery {}
}
