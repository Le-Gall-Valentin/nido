package com.synapsys.api.authentication.domain.model;

public sealed interface LoginResult permits LoginResult.Success, LoginResult.TotpRequired {

    record Success(AuthTokens tokens, UserCredentials credentials) implements LoginResult {}

    record TotpRequired(String challengeId) implements LoginResult {}
}