package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.port.in.CheckPasswordResetTokenUseCase;
import com.nido.api.authentication.application.port.in.ConfirmPasswordResetUseCase;
import com.nido.api.authentication.application.service.PasswordChangeConsequences;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.PasswordResetToken;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.PasswordHasherPort;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.TokenHashPort;
import com.nido.api.authentication.domain.port.out.UserCredentialPort;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Following a reset link: checking it when the page opens, and using it.
 *
 * <p>Using it ends everything the old password opened. Refresh tokens are revoked and access tokens
 * already issued are cut off — a reset is often how someone reacts to losing control of an account,
 * and a session left alive for fifteen more minutes is fifteen minutes too many. Nobody is signed in
 * by it: the next login asks for the new password, and for the TOTP code when there is one — a reset
 * never gets past the second factor.
 */
@ApplicationService
public class PasswordResetHandler implements CheckPasswordResetTokenUseCase, ConfirmPasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetHandler.class);

    private final PasswordResetTokenRepository tokens;
    private final TokenHashPort hasher;
    private final UserProfilePort profiles;
    private final PasswordHasherPort passwordHasher;
    private final UserCredentialPort credentials;
    private final PasswordChangeConsequences consequences;
    private final Clock clock;

    public PasswordResetHandler(PasswordResetTokenRepository tokens, TokenHashPort hasher, UserProfilePort profiles,
                                PasswordHasherPort passwordHasher, UserCredentialPort credentials,
                                PasswordChangeConsequences consequences, Clock clock) {
        this.tokens = tokens;
        this.hasher = hasher;
        this.profiles = profiles;
        this.passwordHasher = passwordHasher;
        this.credentials = credentials;
        this.consequences = consequences;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public void check(String token) {
        accountOf(hash(token).flatMap(tokens::findByHash), clock.instant());
    }

    @Override
    @Transactional
    public void confirm(String token, String newPassword) {
        UserProfile account = accountOf(hash(token).flatMap(tokens::consumeByHash), clock.instant());
        credentials.updatePasswordHash(account.id(), passwordHasher.hash(newPassword));
        consequences.apply(AccountContact.of(account));
        log.info("Password reset for user {} — every session ended", account.id());
    }

    private Optional<String> hash(String token) {
        return token == null || token.isBlank() ? Optional.empty() : Optional.of(hasher.hash(token));
    }

    private UserProfile accountOf(Optional<PasswordResetToken> token, Instant now) {
        return token
            .filter(t -> !t.isExpiredAt(now))
            .flatMap(t -> profiles.findById(t.userId()))
            .filter(UserProfile::isActive)
            .orElseThrow(AuthenticationException.InvalidResetToken::new);
    }
}
