package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.LoginCommand;
import com.nido.api.authentication.application.port.in.LoginUseCase;
import com.nido.api.authentication.domain.model.AuthTokens;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.LoginResult;
import com.nido.api.authentication.domain.model.MailCodeDelivery;
import com.nido.api.authentication.domain.model.MaskedEmail;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.AccessTokenPort;
import com.nido.api.authentication.domain.port.out.PasswordHasherPort;
import com.nido.api.authentication.domain.port.out.PasswordVerifierPort;
import com.nido.api.authentication.domain.port.out.RefreshTokenConfigPort;
import com.nido.api.authentication.domain.port.out.RefreshTokenIssuerPort;
import com.nido.api.authentication.domain.port.out.TwoFactorChallengePort;
import com.nido.api.authentication.domain.port.out.TwoFactorChallengeStorePort;
import com.nido.api.authentication.domain.port.out.UserCredentialsPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import com.nido.api.shared.model.TwoFactorPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@ApplicationService
public class LoginHandler implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);

    private final UserCredentialsPort userCredentialsPort;
    private final PasswordHasherPort passwordHasher;
    private final PasswordVerifierPort passwordVerifier;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenIssuerPort refreshTokenPort;
    private final TwoFactorChallengeStorePort challengeStore;
    private final TwoFactorChallengePort secondFactor;
    private final RefreshTokenConfigPort tokenConfig;
    private final String dummyHash;

    public LoginHandler(UserCredentialsPort userCredentialsPort,
                        PasswordHasherPort passwordHasher,
                        PasswordVerifierPort passwordVerifier,
                        AccessTokenPort accessTokenPort,
                        RefreshTokenIssuerPort refreshTokenPort,
                        RefreshTokenConfigPort tokenConfig,
                        TwoFactorChallengeStorePort challengeStore,
                        TwoFactorChallengePort secondFactor) {
        this.userCredentialsPort = userCredentialsPort;
        this.passwordHasher = passwordHasher;
        this.passwordVerifier = passwordVerifier;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.challengeStore = challengeStore;
        this.secondFactor = secondFactor;
        this.tokenConfig = tokenConfig;
        // Precomputed hash for constant-time dummy comparison — prevents timing-based account enumeration
        this.dummyHash = passwordHasher.hash("nido-timing-sentinel");
    }

    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        var credsOpt = userCredentialsPort.findByIdentifier(command.identifier());

        if (credsOpt.isEmpty()) {
            passwordVerifier.matches(command.password(), dummyHash);
            log.warn("Login attempt for unknown identifier");
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

        Set<TwoFactorMethod> usable = secondFactor.usableMethods(creds.id());
        if (!usable.isEmpty()) {
            // Locked out by wrong codes: no new challenge, and no code mailed into a sign-in that would refuse it.
            if (challengeStore.failedAttempts(creds.id()) >= TwoFactorPolicy.MAX_ATTEMPTS) {
                throw new AuthenticationException.TwoFactorLockedOut(challengeStore.lockoutSecondsLeft(creds.id()));
            }
            String challengeId = challengeStore.createChallenge(creds.id());
            String maskedEmail = usable.contains(TwoFactorMethod.MAIL) ? MaskedEmail.of(creds.email()) : null;
            // The mail as only method: the server sends the code itself. A browser sending it from the code screen
            // would send it twice in development (StrictMode), the second time refused for being too soon.
            MailCodeDelivery mailCode = usable.equals(Set.of(TwoFactorMethod.MAIL))
                ? secondFactor.sendMailCode(creds.id(), challengeId)
                : null;
            if (!(mailCode instanceof MailCodeDelivery.Unavailable)) {
                log.info("Two-factor challenge created for user: {}", creds.id());
                return new LoginResult.TwoFactorRequired(challengeId, creds.username(), TwoFactorMethod.ordered(usable),
                    maskedEmail, mailCode);
            }
            // Mail went off between the check and the send: the method is paused after all, and a paused
            // method lets the password through.
            challengeStore.invalidateChallenge(challengeId);
        }

        log.info("Successful login for user: {}", creds.id());
        AuthTokens tokens = new AuthTokens(
            accessTokenPort.generate(creds),
            refreshTokenPort.generate(creds, tokenConfig.refreshTokenExpiryDays())
        );
        return new LoginResult.Success(tokens, creds, TwoFactorMethod.ordered(secondFactor.activeMethods(creds.id())));
    }
}
