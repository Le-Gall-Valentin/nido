package com.nido.api.mfa.domain.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * An enrolment that has been started but not yet proven.
 *
 * <p>Kept apart from {@link UserTotpQueryPort} and the record it reads because the two are not the
 * same kind of thing. A confirmed TOTP secret is durable state: it protects an account and must
 * survive anything. A secret that has only been displayed as a QR code protects nothing yet — it is
 * a step in a conversation, and it should stop existing when that conversation stops.
 *
 * <p>Storing it as though it were durable is what produced every symptom this port exists to
 * remove: a secret that outlived the enrolment by years, a user who could never restart one, and a
 * cancellation that had to fight a database transaction to take effect.
 *
 * <p>Implementations must expire an enrolment on their own. Nothing in the application layer polls
 * for it, and nothing should have to.
 */
public interface PendingTotpEnrolmentPort {

    /**
     * Records this enrolment unless one is already in progress.
     *
     * @return false when an enrolment was already under way, in which case it is left untouched and
     *         {@link #find(UUID)} returns it. Two tabs on the enrolment page must not end up
     *         showing two different QR codes, or the user scans one and confirms against the other.
     */
    boolean startIfAbsent(UUID userId, String secret);

    /** Empty when no enrolment is under way, or when the one that was has expired. */
    Optional<String> find(UUID userId);

    /** Idempotent: discarding an enrolment that is not there is not an error. */
    void discard(UUID userId);
}
