package com.nido.api.mfa.application.method;

import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;

import java.util.UUID;

/** A method whose codes Nido sends, rather than one that shows its own: it can be asked for a code. */
public interface CodeSendingMethod extends TwoFactorMethodHandler {

    /**
     * Sends a code for this purpose, bound to {@code binding}: the challenge id for {@link CodePurpose#LOGIN},
     * the account id for {@link CodePurpose#DISABLE}. Never throws for a refusal: see {@link CodeDelivery}.
     */
    CodeDelivery sendCode(UUID userId, CodePurpose purpose, String binding);
}
