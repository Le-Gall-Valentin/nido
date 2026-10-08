package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.model.MailCodeDelivery;
import com.nido.api.authentication.domain.model.SecondFactorCheck;
import com.nido.api.authentication.domain.port.out.TwoFactorChallengePort;
import com.nido.api.mfa.application.port.in.GetTwoFactorMethodsUseCase;
import com.nido.api.mfa.application.port.in.TwoFactorChallengeUseCase;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class MfaTwoFactorChallengeAdapter implements TwoFactorChallengePort {

    private final GetTwoFactorMethodsUseCase methods;
    private final TwoFactorChallengeUseCase challenge;

    public MfaTwoFactorChallengeAdapter(GetTwoFactorMethodsUseCase methods, TwoFactorChallengeUseCase challenge) {
        this.methods = methods;
        this.challenge = challenge;
    }

    @Override
    public Set<TwoFactorMethod> activeMethods(UUID userId) {
        return methods.activeMethods(userId);
    }

    @Override
    public Set<TwoFactorMethod> usableMethods(UUID userId) {
        return challenge.usableMethods(userId);
    }

    @Override
    public MailCodeDelivery sendMailCode(UUID userId, String challengeId) {
        return switch (challenge.sendMailCode(userId, challengeId)) {
            case CodeDelivery.Sent sent -> new MailCodeDelivery.Sent(sent.resendAfterSeconds());
            case CodeDelivery.TooSoon tooSoon -> new MailCodeDelivery.TooSoon(tooSoon.retryAfterSeconds());
            case CodeDelivery.LimitReached limit -> new MailCodeDelivery.LimitReached(limit.retryAfterSeconds());
            case CodeDelivery.Unavailable unavailable -> new MailCodeDelivery.Unavailable();
        };
    }

    @Override
    public SecondFactorCheck verify(UUID userId, TwoFactorMethod method, String challengeId, String code) {
        return switch (challenge.verify(userId, method, challengeId, code)) {
            case SUCCESS -> SecondFactorCheck.SUCCESS;
            case INVALID -> SecondFactorCheck.INVALID;
            case REPLAYED -> SecondFactorCheck.REPLAYED;
        };
    }
}
