package com.synapsys.api.authentication.application.handler;

import com.synapsys.api.authentication.application.dto.LoginCommand;
import com.synapsys.api.authentication.application.port.in.LoginUseCase;
import com.synapsys.api.authentication.domain.model.*;
import com.synapsys.api.authentication.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class LoginHandler implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);

    private final UserCredentialsPort userCredentialsPort;
    private final PasswordHasherPort passwordHasher;
    private final PasswordVerifierPort passwordVerifier;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenIssuerPort refreshTokenPort;
    private final TotpChallengeStorePort totpChallengeStore;
    private final TotpStatusQueryPort totpStatusQuery;
    private final int refreshTokenExpiryDays;
    private final String dummyHash;

    public LoginHandler(UserCredentialsPort userCredentialsPort,
                        PasswordHasherPort passwordHasher,
                        PasswordVerifierPort passwordVerifier,
                        AccessTokenPort accessTokenPort,
                        RefreshTokenIssuerPort refreshTokenPort,
                        RefreshTokenConfigPort tokenConfig,
                        TotpChallengeStorePort totpChallengeStore,
                        TotpStatusQueryPort totpStatusQuery) {
        this.userCredentialsPort = userCredentialsPort;
        this.passwordHasher = passwordHasher;
        this.passwordVerifier = passwordVerifier;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.totpChallengeStore = totpChallengeStore;
        this.totpStatusQuery = totpStatusQuery;
        this.refreshTokenExpiryDays = tokenConfig.refreshTokenExpiryDays();
        // Precomputed hash for constant-time dummy comparison — prevents timing-based username enumeration
        this.dummyHash = passwordHasher.hash("synapsys-timing-sentinel");
    }

    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        var credsOpt = userCredentialsPort.findByUsername(command.username());

        if (credsOpt.isEmpty()) {
            passwordVerifier.matches(command.password(), dummyHash);
            log.warn("Login attempt for unknown username");
            throw new AuthenticationException.InvalidCredentials();
        }

        UserCredentials creds = credsOpt.get();

        // Password is verified before isActive to avoid disclosing account status on wrong credentials.
        if (!passwordVerifier.matches(command.password(), creds.passwordHash())) {
            log.warn("Failed login attempt — invalid credentials");
            throw new AuthenticationException.InvalidCredentials();
        }

        if (!creds.isActive()) {
            log.warn("Login attempt on inactive account");
            throw new AuthenticationException.UserNotActive();
        }

        if (totpStatusQuery.isTotpEnabled(creds.id())) {
            String challengeId = totpChallengeStore.createChallenge(creds.id());
            log.info("TOTP challenge created for user: {}", creds.id());
            return new LoginResult.TotpRequired(challengeId);
        }

        log.info("Successful login for user: {}", creds.id());
        AuthTokens tokens = new AuthTokens(
            accessTokenPort.generate(creds),
            refreshTokenPort.generate(creds, refreshTokenExpiryDays)
        );
        return new LoginResult.Success(tokens, creds);
    }
}