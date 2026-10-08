package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.port.in.SendChallengeMailCodeUseCase;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.MailCodeDelivery;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.TwoFactorChallengePort;
import com.nido.api.authentication.domain.port.out.TwoFactorChallengeStorePort;
import com.nido.api.authentication.domain.port.out.UserCredentialsPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import com.nido.api.shared.model.TwoFactorPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** "Code par mail" chosen on the choice screen, or "Renvoyer": the code for this sign-in, bound to its challenge. */
@ApplicationService
public class SendChallengeMailCodeHandler implements SendChallengeMailCodeUseCase {

    private final TwoFactorChallengeStorePort challenges;
    private final UserCredentialsPort credentials;
    private final TwoFactorChallengePort secondFactor;

    public SendChallengeMailCodeHandler(TwoFactorChallengeStorePort challenges, UserCredentialsPort credentials,
                                        TwoFactorChallengePort secondFactor) {
        this.challenges = challenges;
        this.credentials = credentials;
        this.secondFactor = secondFactor;
    }

    @Override
    @Transactional
    public long send(String challengeId) {
        UUID userId = challenges.resolveChallenge(challengeId)
            .orElseThrow(AuthenticationException.TwoFactorChallengeExpired::new);
        UserCredentials account = credentials.findById(userId).orElseThrow(AuthenticationException.UserNotFound::new);
        if (!account.isActive()) {
            throw new AuthenticationException.UserNotActive();
        }
        if (!secondFactor.activeMethods(userId).contains(TwoFactorMethod.MAIL)) {
            throw new AuthenticationException.MethodNotEnabled();
        }
        if (!secondFactor.usableMethods(userId).contains(TwoFactorMethod.MAIL)) {
            throw new AuthenticationException.MethodUnavailable();
        }
        // Locked out by wrong codes, as at /login: no code mailed into a sign-in that would refuse it.
        if (challenges.failedAttempts(userId) >= TwoFactorPolicy.MAX_ATTEMPTS) {
            throw new AuthenticationException.TwoFactorLockedOut(challenges.lockoutSecondsLeft(userId));
        }
        return switch (secondFactor.sendMailCode(userId, challengeId)) {
            case MailCodeDelivery.Sent sent -> sent.resendAfterSeconds();
            case MailCodeDelivery.TooSoon tooSoon -> throw new AuthenticationException.MailCodeRefused(true, tooSoon.retryAfterSeconds());
            case MailCodeDelivery.LimitReached limit -> throw new AuthenticationException.MailCodeRefused(false, limit.retryAfterSeconds());
            case MailCodeDelivery.Unavailable unavailable -> throw new AuthenticationException.MethodUnavailable();
        };
    }
}
