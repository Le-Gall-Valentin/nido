package com.synapsys.api.mfa.domain.model;

public record TotpSetupResult(String secret, String otpauthUri) {}