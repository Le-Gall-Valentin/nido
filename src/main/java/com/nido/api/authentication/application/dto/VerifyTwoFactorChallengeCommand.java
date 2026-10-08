package com.nido.api.authentication.application.dto;

import com.nido.api.shared.model.TwoFactorMethod;

public record VerifyTwoFactorChallengeCommand(String challengeId, TwoFactorMethod method, String code) {
    @Override
    public String toString() {
        return "VerifyTwoFactorChallengeCommand[method=" + method + ", code=***]";
    }
}
