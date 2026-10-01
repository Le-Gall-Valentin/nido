package com.nido.api.authentication.domain.model;

public sealed interface LoginResult permits LoginResult.Success, LoginResult.TotpRequired {

    record Success(AuthTokens tokens, UserCredentials credentials) implements LoginResult {}

    /** {@code username} lets the code screen greet the account, even when the person typed an address. */
    record TotpRequired(String challengeId, String username) implements LoginResult {}
}