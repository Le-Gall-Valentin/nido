package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.model.EnrolmentStarted;
import com.nido.api.mfa.domain.model.MfaException;
import com.nido.api.mfa.domain.port.out.AccountAddressPort;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import com.nido.api.mfa.domain.port.out.TotpCodeReplayPort;
import com.nido.api.mfa.domain.port.out.TotpCodeValidatorPort;
import com.nido.api.mfa.domain.port.out.TotpConfirmAttemptPort;
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
 * codes abandon it, a code is never accepted twice.
 */
@ApplicationService
public class AppMethod implements TwoFactorMethodHandler {

    private final TwoFactorMethodStorePort store;
    private final TotpSecretGeneratorPort secrets;
    private final TotpUriBuilderPort uris;
    private final PendingTotpEnrolmentPort pending;
    private final TotpCodeValidatorPort validator;
    private final TotpCodeReplayPort replay;
    private final TotpConfirmAttemptPort confirmAttempts;
    private final AccountAddressPort addresses;

    public AppMethod(TwoFactorMethodStorePort store, TotpSecretGeneratorPort secrets, TotpUriBuilderPort uris,
                     PendingTotpEnrolmentPort pending, TotpCodeValidatorPort validator, TotpCodeReplayPort replay,
                     TotpConfirmAttemptPort confirmAttempts, AccountAddressPort addresses) {
        this.store = store;
        this.secrets = secrets;
        this.uris = uris;
        this.pending = pending;
        this.validator = validator;
        this.replay = replay;
        this.confirmAttempts = confirmAttempts;
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
    public boolean deliversCodes() {
        return false;
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
            int attempts = confirmAttempts.incrementAndGetAttempts(userId);
            if (attempts >= TwoFactorPolicy.MAX_ATTEMPTS) {
                pending.discard(userId);
                confirmAttempts.clearAttempts(userId);
                throw new MfaException.ConfirmMaxAttemptsExceeded();
            }
            throw new MfaException.CodeInvalid();
        }
        if (!replay.markCodeUsedIfAbsent(userId, code)) {
            throw new MfaException.CodeInvalid();
        }
        confirmAttempts.clearAttempts(userId);
        // The enrolment is forgotten by the caller once the secret is written where it will survive.
        return Optional.of(secret);
    }

    @Override
    public CodeDelivery sendCode(UUID userId, CodePurpose purpose, String binding) {
        // The app shows its own codes: there is nothing to send.
        return new CodeDelivery.Unavailable();
    }

    @Override
    public CodeCheck check(UUID userId, CodePurpose purpose, String binding, String code) {
        Optional<String> secret = store.appSecret(userId);
        if (secret.isEmpty() || !validator.isValid(secret.get(), code)) {
            return CodeCheck.INVALID;
        }
        return replay.markCodeUsedIfAbsent(userId, code) ? CodeCheck.SUCCESS : CodeCheck.REPLAYED;
    }

    @Override
    public void forgetPending(UUID userId) {
        pending.discard(userId);
        confirmAttempts.clearAttempts(userId);
    }
}
