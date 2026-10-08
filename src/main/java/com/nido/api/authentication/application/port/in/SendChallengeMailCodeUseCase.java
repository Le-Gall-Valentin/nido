package com.nido.api.authentication.application.port.in;

public interface SendChallengeMailCodeUseCase {

    /** @return the seconds before another code can be asked for */
    long send(String challengeId);
}
