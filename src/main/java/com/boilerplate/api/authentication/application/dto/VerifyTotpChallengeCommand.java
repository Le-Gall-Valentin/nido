package com.boilerplate.api.authentication.application.dto;

public record VerifyTotpChallengeCommand(String challengeId, String code) {}