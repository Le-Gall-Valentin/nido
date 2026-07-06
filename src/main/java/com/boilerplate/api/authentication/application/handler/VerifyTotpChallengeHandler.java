package com.boilerplate.api.authentication.application.handler;

import com.boilerplate.api.authentication.application.dto.VerifyTotpChallengeCommand;
import com.boilerplate.api.authentication.application.port.in.VerifyTotpChallengeUseCase;
import com.boilerplate.api.authentication.domain.model.AuthTokens;
import com.boilerplate.api.authentication.domain.model.AuthenticationException;
import com.boilerplate.api.authentication.domain.model.LoginResult;
import com.boilerplate.api.authentication.domain.model.UserCredentials;
import com.boilerplate.api.authentication.domain.port.out.*;
import com.boilerplate.api.authentication.domain.model.*;
import com.boilerplate.api.authentication.domain.port.out.*;
import com.boilerplate.api.shared.annotation.ApplicationService;
import com.boilerplate.api.shared.model.TotpPolicy;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

// @Transactional is required here because RefreshTokenGenerator (RefreshTokenIssuerPort)
// uses Propagation.MANDATORY and must run inside an existing transaction.
// Redis operations (challenge store) are not part of this transaction and not rolled back
// on JPA failure — this is an accepted trade-off documented in the architecture decisions.
@ApplicationService
public class VerifyTotpChallengeHandler implements VerifyTotpChallengeUseCase {

    private final TotpChallengeStorePort challengeStore;
    private final MfaTotpVerifierPort mfaVerifier;
    private final UserCredentialsPort userCredentialsPort;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenIssuerPort refreshTokenPort;
    private final int refreshTokenExpiryDays;

    public VerifyTotpChallengeHandler(TotpChallengeStorePort challengeStore,
                                      MfaTotpVerifierPort mfaVerifier,
                                      UserCredentialsPort userCredentialsPort,
                                      AccessTokenPort accessTokenPort,
                                      RefreshTokenIssuerPort refreshTokenPort,
                                      RefreshTokenConfigPort tokenConfig) {
        this.challengeStore = challengeStore;
        this.mfaVerifier = mfaVerifier;
        this.userCredentialsPort = userCredentialsPort;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.refreshTokenExpiryDays = tokenConfig.refreshTokenExpiryDays();
    }

    @Override
    @Transactional
    public LoginResult.Success verify(VerifyTotpChallengeCommand command) {
        UUID userId = challengeStore.resolveChallenge(command.challengeId())
            .orElseThrow(AuthenticationException.TotpChallengeExpired::new);

        UserCredentials creds = userCredentialsPort.findById(userId)
            .orElseThrow(AuthenticationException.UserNotFound::new);

        if (!creds.isActive()) throw new AuthenticationException.UserNotActive();

        switch (mfaVerifier.verifyAndConsume(userId, command.code())) {
            case REPLAYED -> throw new AuthenticationException.TotpCodeInvalid();
            case INVALID -> {
                int attempts = challengeStore.incrementFailedAttempts(command.challengeId());
                if (attempts >= TotpPolicy.MAX_ATTEMPTS) {
                    challengeStore.invalidateChallenge(command.challengeId());
                    throw new AuthenticationException.TotpMaxAttemptsExceeded();
                }
                throw new AuthenticationException.TotpCodeInvalid();
            }
            case SUCCESS -> {}
        }

        challengeStore.invalidateChallenge(command.challengeId());

        AuthTokens tokens = new AuthTokens(
            accessTokenPort.generate(creds),
            refreshTokenPort.generate(creds, refreshTokenExpiryDays)
        );
        return new LoginResult.Success(tokens, creds);
    }
}