package com.nido.api.authentication.domain.model;

import com.nido.api.shared.model.TwoFactorMethod;

import java.util.List;

public sealed interface LoginResult permits LoginResult.Success, LoginResult.TwoFactorRequired {

    /** {@code twoFactorMethods}: the methods on, paused ones included — the client proposes none to such an account. */
    record Success(AuthTokens tokens, UserCredentials credentials, List<TwoFactorMethod> twoFactorMethods) implements LoginResult {}

    /**
     * {@code username} greets the account even when the person typed an address; {@code maskedEmail} only when
     * the mail is among the methods; {@code mailCode} only when the mail was the only method and the server
     * sent the code itself.
     */
    record TwoFactorRequired(String challengeId, String username, List<TwoFactorMethod> methods, String maskedEmail,
                             MailCodeDelivery mailCode) implements LoginResult {}
}
