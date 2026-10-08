package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Optional;
import java.util.UUID;

/**
 * What a second-factor method knows how to do. The use cases are written once against this contract and
 * never ask which method they hold; what genuinely differs — how a method starts — is said by the type of
 * {@link EnrolmentStarted} rather than forced into one shape.
 */
public interface TwoFactorMethodHandler {

    TwoFactorMethod method();

    /** Whether it can be turned on or asked for now — the mail method is not while mail is off. */
    boolean usableNow();

    /** Whether this method sends its codes, and so can be asked for one. */
    boolean deliversCodes();

    EnrolmentStarted startEnrolment(UUID userId);

    /**
     * Proves the enrolment. Throws {@code CodeInvalid}, {@code ConfirmMaxAttemptsExceeded} or
     * {@code EnrolmentNotStarted} when it cannot.
     *
     * @return what must be kept for the method to work — the application's secret — or empty
     */
    Optional<String> confirmEnrolment(UUID userId, String code);

    /**
     * Sends a code for this purpose, bound to {@code binding}: the challenge id for {@link CodePurpose#LOGIN},
     * the account id for {@link CodePurpose#DISABLE}. {@link CodeDelivery.Unavailable} for a method that does
     * not deliver codes. Never throws for a refusal: see {@link CodeDelivery}.
     */
    CodeDelivery sendCode(UUID userId, CodePurpose purpose, String binding);

    CodeCheck check(UUID userId, CodePurpose purpose, String binding, String code);

    /** Drops what is under way for this account: an enrolment, the codes sent. */
    void forgetPending(UUID userId);
}
