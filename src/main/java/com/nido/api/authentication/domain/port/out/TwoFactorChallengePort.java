package com.nido.api.authentication.domain.port.out;

import com.nido.api.authentication.domain.model.MailCodeDelivery;
import com.nido.api.authentication.domain.model.SecondFactorCheck;
import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Set;
import java.util.UUID;

/** The second factor of a sign-in, as authentication needs it: defined here, answered by mfa through an adapter. */
public interface TwoFactorChallengePort {

    /** On, paused or not — what the browser is told the account has. */
    Set<TwoFactorMethod> activeMethods(UUID userId);

    /** On and usable now — what the sign-in may ask for. */
    Set<TwoFactorMethod> usableMethods(UUID userId);

    MailCodeDelivery sendMailCode(UUID userId, String challengeId);

    /** REPLAYED: a right code already used — not counted as a failure. */
    SecondFactorCheck verify(UUID userId, TwoFactorMethod method, String challengeId, String code);
}
