package com.nido.api.mfa.domain.model;

/**
 * What came of asking for a code to be sent. A refusal is an answer, not an exception: sign-in asks inside
 * its own transaction and must carry on — with the code screen saying why — when the code cannot leave.
 */
public sealed interface CodeDelivery
    permits CodeDelivery.Sent, CodeDelivery.TooSoon, CodeDelivery.LimitReached, CodeDelivery.Unavailable {

    /** Queued; another can be asked for after {@code resendAfterSeconds}. */
    record Sent(long resendAfterSeconds) implements CodeDelivery {}

    /** One was sent for the same thing moments ago — it still works. */
    record TooSoon(long retryAfterSeconds) implements CodeDelivery {}

    /** The account has had all the mails of code its window allows. */
    record LimitReached(long retryAfterSeconds) implements CodeDelivery {}

    /** Nothing can be sent: mail is off, the account has no address, or the method sends no code. */
    record Unavailable() implements CodeDelivery {}

    /** For a person asking on a screen: the seconds before a resend, or the refusal as the error it is. */
    default long resendAfterOrThrow() {
        return switch (this) {
            case Sent sent -> sent.resendAfterSeconds();
            case TooSoon tooSoon -> throw new MfaException.ResendTooSoon(tooSoon.retryAfterSeconds());
            case LimitReached limit -> throw new MfaException.SendLimitReached(limit.retryAfterSeconds());
            case Unavailable unavailable -> throw new MfaException.MethodUnavailable();
        };
    }
}
