package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Optional;
import java.util.UUID;

/**
 * What every second-factor method knows how to do. The use cases are written once against this contract and
 * never ask which method they hold; what genuinely differs — how a method starts — is said by the type of
 * {@link EnrolmentStarted} rather than forced into one shape, and a method whose codes are sent says so by
 * being a {@link CodeSendingMethod}.
 */
public interface TwoFactorMethodHandler {

    TwoFactorMethod method();

    /** Whether it can be turned on or asked for now — the mail method is not while mail is off. */
    boolean usableNow();

    EnrolmentStarted startEnrolment(UUID userId);

    /**
     * Proves the enrolment. Throws {@code CodeInvalid}, {@code ConfirmMaxAttemptsExceeded} or
     * {@code EnrolmentNotStarted} when it cannot.
     *
     * @return what must be kept for the method to work — the application's secret — or empty
     */
    Optional<String> confirmEnrolment(UUID userId, String code);

    /**
     * Wrong codes outside sign-in are counted here, five at most; at sign-in, the account's counter does it.
     * See {@link CodeCheck} for what each answer means.
     */
    CodeCheck check(UUID userId, CodePurpose purpose, String binding, String code);

    /** Drops what is under way for this account: an enrolment, the codes sent, the wrong codes counted. */
    void forgetPending(UUID userId);
}
