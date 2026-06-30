package com.synapsys.api.authentication.application.dto;

public record VerifyTotpChallengeCommand(String challengeId, String code) {}