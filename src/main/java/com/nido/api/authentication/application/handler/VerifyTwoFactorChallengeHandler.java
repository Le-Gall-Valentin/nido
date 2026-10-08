package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.VerifyTwoFactorChallengeCommand;
import com.nido.api.authentication.application.port.in.VerifyTwoFactorChallengeUseCase;
import com.nido.api.authentication.domain.model.AuthTokens;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.LoginResult;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.AccessTokenPort;
import com.nido.api.authentication.domain.port.out.RefreshTokenConfigPort;
import com.nido.api.authentication.domain.port.out.RefreshTokenIssuerPort;
import com.nido.api.authentication.domain.port.out.TwoFactorChallengePort;
import com.nido.api.authentication.domain.port.out.TwoFactorChallengeStorePort;
import com.nido.api.authentication.domain.port.out.UserCredentialsPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import com.nido.api.shared.model.TwoFactorPolicy;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

// @Transactional is required here because RefreshTokenGenerator (RefreshTokenIssuerPort)
// uses Propagation.MANDATORY and must run inside an existing transaction.
// Redis operations (challenge store) are not part of this transaction and not rolled back
// on JPA failure — this is an accepted trade-off documented in the architecture decisions.
@ApplicationService
public class VerifyTwoFactorChallengeHandler implements VerifyTwoFactorChallengeUseCase {

    private final TwoFactorChallengeStorePort challengeStore;
    private final TwoFactorChallengePort secondFactor;
    private final UserCredentialsPort userCredentialsPort;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenIssuerPort refreshTokenPort;
    private final RefreshTokenConfigPort tokenConfig;

    public VerifyTwoFactorChallengeHandler(TwoFactorChallengeStorePort challengeStore,
                                      TwoFactorChallengePort secondFactor,
                                      UserCredentialsPort userCredentialsPort,
                                      AccessTokenPort accessTokenPort,
                                      RefreshTokenIssuerPort refreshTokenPort,
                                      RefreshTokenConfigPort tokenConfig) {
        this.challengeStore = challengeStore;
        this.secondFactor = secondFactor;
        this.userCredentialsPort = userCredentialsPort;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.tokenConfig = tokenConfig;
    }

    @Override
    @Transactional
    public LoginResult.Success verify(VerifyTwoFactorChallengeCommand command) {
        UUID userId = challengeStore.resolveChallenge(command.challengeId())
            .orElseThrow(AuthenticationException.TwoFactorChallengeExpired::new);

        UserCredentials creds = userCredentialsPort.findById(userId)
            .orElseThrow(AuthenticationException.UserNotFound::new);

        if (!creds.isActive()) throw new AuthenticationException.UserNotActive();

        // Recomputed at each step: an administrator may have removed the method, or mail been switched off,
        // since the challenge was created.
        if (!secondFactor.activeMethods(userId).contains(command.method())) {
            throw new AuthenticationException.MethodNotEnabled();
        }
        if (!secondFactor.usableMethods(userId).contains(command.method())) {
            throw new AuthenticationException.MethodUnavailable();
        }

        // Refused before the code is even looked at, and without recording anything. The
        // counter belongs to the account, so an attempt made while already locked out must not
        // touch it: renewing the window on a refused attempt would let a caller hold the
        // account locked indefinitely at no cost, turning a brute-force guard into a way to
        // deny its owner service.
        if (challengeStore.failedAttempts(userId) >= TwoFactorPolicy.MAX_ATTEMPTS) {
            throw new AuthenticationException.TwoFactorMaxAttemptsExceeded();
        }

        switch (secondFactor.verify(userId, command.method(), command.challengeId(), command.code())) {
            case REPLAYED -> throw new AuthenticationException.TwoFactorCodeInvalid();
            case INVALID -> {
                int attempts = challengeStore.recordFailedAttempt(userId);
                if (attempts >= TwoFactorPolicy.MAX_ATTEMPTS) {
                    challengeStore.invalidateChallenge(command.challengeId());
                    throw new AuthenticationException.TwoFactorMaxAttemptsExceeded();
                }
                throw new AuthenticationException.TwoFactorCodeInvalid();
            }
            case SUCCESS -> {}
        }

        challengeStore.invalidateChallenge(command.challengeId());
        // Proving possession of the authenticator clears the slate: someone who mistyped three
        // codes before getting it right must not carry those failures into their next login.
        challengeStore.clearFailedAttempts(userId);

        AuthTokens tokens = new AuthTokens(
            accessTokenPort.generate(creds),
            refreshTokenPort.generate(creds, tokenConfig.refreshTokenExpiryDays())
        );
        return new LoginResult.Success(tokens, creds, TwoFactorMethod.ordered(secondFactor.activeMethods(userId)));
    }
}