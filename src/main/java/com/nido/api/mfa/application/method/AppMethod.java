package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.port.out.AccountAddressPort;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import com.nido.api.mfa.domain.port.out.TotpCodeReplayPort;
import com.nido.api.mfa.domain.port.out.TotpCodeValidatorPort;
import com.nido.api.mfa.domain.port.out.TotpAttemptPort;
import com.nido.api.mfa.domain.port.out.TotpSecretGeneratorPort;
import com.nido.api.mfa.domain.port.out.TotpUriBuilderPort;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import com.nido.api.shared.model.TwoFactorPolicy;

import java.util.Optional;
import java.util.UUID;

/**
 * The authenticator app: a TOTP secret shown once as a QR code, then codes the app computes itself. Its rules
 * are the ones Nido has had since the beginning — an enrolment lives in Redis until proven, five wrong first
 * codes abandon it, a code is never accepted twice — and five wrong codes to turn it off hold it on for a while.
 */
@ApplicationService
public class AppMethod implements TwoFactorMethodHandler {

    private final TwoFactorMethodStorePort store;
    private final TotpSecretGeneratorPort secrets;
    private final TotpUriBuilderPort uris;
    private final PendingTotpEnrolmentPort pending;
    private final TotpCodeValidatorPort validator;
    private final TotpCodeReplayPort replay;
    private final TotpAttemptPort attempts;
    private final AccountAddressPort addresses;

    public AppMethod(TwoFactorMethodStorePort store, TotpSecretGeneratorPort secrets, TotpUriBuilderPort uris,
                     PendingTotpEnrolmentPort pending, TotpCodeValidatorPort validator, TotpCodeReplayPort replay,
                     TotpAttemptPort attempts, AccountAddressPort addresses) {
        this.store = store;
        this.secrets = secrets;
        this.uris = uris;
        this.pending = pending;
        this.validator = validator;
        this.replay = replay;
        this.attempts = attempts;
        this.addresses = addresses;
    }

    @Override
    public TwoFactorMethod method() {
        return TwoFactorMethod.APP;
    }

    @Override
    public boolean usableNow() {
        return true;
    }

    @Override
    public EnrolmentStarted startEnrolment(UUID userId) {
        String address = addresses.addressOf(userId).orElseThrow(MfaException.UserNotFound::new);
        String candidate = secrets.generateSecret();
        if (pending.startIfAbsent(userId, candidate)) {
            return new EnrolmentStarted.AppEnrolment(candidate, uris.buildOtpauthUri(candidate, address));
        }
        // An enrolment was already under way: hand back the one being shown rather than a second QR code.
        // It can still have expired between the two calls, in which case there is nothing to hand back.
        String existing = pending.find(userId).orElseThrow(MfaException.EnrolmentNotStarted::new);
        return new EnrolmentStarted.AppEnrolment(existing, uris.buildOtpauthUri(existing, address));
    }

    @Override
    public Optional<String> confirmEnrolment(UUID userId, String code) {
        String secret = pending.find(userId).orElseThrow(MfaException.EnrolmentNotStarted::new);
        if (!validator.isValid(secret, code)) {
            if (attempts.recordFailure(userId, CodePurpose.ENROL) >= TwoFactorPolicy.MAX_ATTEMPTS) {
                pending.discard(userId);
                attempts.clear(userId, CodePurpose.ENROL);
                throw new MfaException.ConfirmMaxAttemptsExceeded();
            }
            throw new MfaException.CodeInvalid();
        }
        if (!replay.markCodeUsedIfAbsent(userId, code)) {
            throw new MfaException.CodeInvalid();
        }
        attempts.clear(userId, CodePurpose.ENROL);
        // The enrolment is forgotten by the caller once the secret is written where it will survive.
        return Optional.of(secret);
    }

    @Override
    public CodeCheck check(UUID userId, CodePurpose purpose, String binding, String code) {
        boolean counted = purpose != CodePurpose.LOGIN;
        // Spent: refused before the code is read, and not counted — the quarter of an hour runs from the fifth.
        if (counted && attempts.failures(userId, purpose) >= TwoFactorPolicy.MAX_ATTEMPTS) {
            return CodeCheck.SPENT;
        }
        Optional<String> secret = store.appSecret(userId);
        if (secret.isEmpty() || !validator.isValid(secret.get(), code)) {
            if (counted && attempts.recordFailure(userId, purpose) >= TwoFactorPolicy.MAX_ATTEMPTS) {
                return CodeCheck.SPENT;
            }
            return CodeCheck.INVALID;
        }
        if (!replay.markCodeUsedIfAbsent(userId, code)) {
            return CodeCheck.REPLAYED;
        }
        if (counted) {
            attempts.clear(userId, purpose);
        }
        return CodeCheck.SUCCESS;
    }

    @Override
    public void forgetPending(UUID userId) {
        pending.discard(userId);
        attempts.clear(userId, CodePurpose.ENROL);
        attempts.clear(userId, CodePurpose.DISABLE);
    }
}
