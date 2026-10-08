package com.nido.api.mfa.application.port.in;

import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Set;
import java.util.UUID;

/**
 * The second step of a sign-in, as authentication asks it. Nothing here throws for a refusal: sign-in calls it
 * inside its own transaction, which an exception crossing a transactional method would mark for rollback.
 */
public interface TwoFactorChallengeUseCase {

    /** The methods on and usable now — what the sign-in may ask for. */
    Set<TwoFactorMethod> usableMethods(UUID userId);

    /** Sends the sign-in code, bound to this challenge. */
    CodeDelivery sendMailCode(UUID userId, String challengeId);

    CodeCheck verify(UUID userId, TwoFactorMethod method, String challengeId, String code);
}
