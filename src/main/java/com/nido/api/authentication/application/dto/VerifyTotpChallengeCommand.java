package com.nido.api.authentication.application.dto;

public record VerifyTotpChallengeCommand(String challengeId, String code) {}