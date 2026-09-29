package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.port.in.RequestPasswordResetUseCase;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.PasswordResetRules;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.ResetTokenGeneratorPort;
import com.nido.api.authentication.domain.port.out.TokenHashPort;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * "Forgot password": finds the account behind a username or an address and mails it a link.
 *
 * <p>Whatever happens, the caller learns nothing — the route answers the same way for an account, a
 * stranger, a deactivated account and a cooldown — so nothing here throws for those cases, and the
 * logs never repeat what was typed.
 *
 * <p>The username is looked up first because a username may itself contain an '@'. An address two
 * accounts share (they may differ only by letter case) sends nothing: guessing would mail a link to
 * the wrong person.
 */
@ApplicationService
public class RequestPasswordResetHandler implements RequestPasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(RequestPasswordResetHandler.class);

    private final UserProfilePort profiles;
    private final PasswordResetTokenRepository tokens;
    private final ResetTokenGeneratorPort generator;
    private final TokenHashPort hasher;
    private final AccountMailPort mail;
    private final Clock clock;

    public RequestPasswordResetHandler(UserProfilePort profiles, PasswordResetTokenRepository tokens,
                                       ResetTokenGeneratorPort generator, TokenHashPort hasher,
                                       AccountMailPort mail, Clock clock) {
        this.profiles = profiles;
        this.tokens = tokens;
        this.generator = generator;
        this.hasher = hasher;
        this.mail = mail;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void request(String identifier) {
        String typed = identifier == null ? "" : identifier.strip();
        if (typed.isEmpty()) {
            return;
        }
        Optional<UserProfile> found = findAccount(typed);
        if (found.isEmpty() || !found.get().isActive()) {
            log.info("Password reset asked for no active account");
            return;
        }
        UserProfile account = found.get();
        Instant now = clock.instant();
        if (tokens.latestIssuedAt(account.id()).filter(last -> PasswordResetRules.inCooldown(last, now)).isPresent()) {
            log.info("Password reset for user {} held back: a link went out less than {} ago",
                account.id(), PasswordResetRules.COOLDOWN);
            return;
        }
        tokens.deleteAllForUser(account.id());
        String token = generator.newToken();
        Instant expiresAt = now.plus(PasswordResetRules.VALIDITY);
        tokens.save(account.id(), hasher.hash(token), now, expiresAt);
        mail.passwordResetRequested(AccountContact.of(account), token, expiresAt, PasswordResetRules.VALIDITY);
        log.info("Password reset link issued for user {}", account.id());
    }

    private Optional<UserProfile> findAccount(String identifier) {
        Optional<UserProfile> byUsername = profiles.findByUsername(identifier);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        List<UserProfile> byAddress = profiles.findByEmailIgnoreCase(identifier);
        if (byAddress.size() > 1) {
            log.warn("Password reset asked for an address {} accounts share: nothing sent", byAddress.size());
            return Optional.empty();
        }
        return byAddress.stream().findFirst();
    }
}
