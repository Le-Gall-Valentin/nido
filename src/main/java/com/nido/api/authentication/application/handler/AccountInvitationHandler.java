package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.port.in.AcceptAccountInvitationUseCase;
import com.nido.api.authentication.application.port.in.CheckAccountInvitationUseCase;
import com.nido.api.authentication.domain.model.AccountInvitation;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.domain.port.out.PasswordHasherPort;
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
 * Following an invitation link: checking it when the page opens, and choosing the first password with it.
 *
 * <p>Accepting creates the account's credentials and removes the invitation in one transaction: of two
 * acceptances racing with the same link, one gets it — consumeByHash locks the row — and the other is
 * refused; a refused one rolls back and leaves the invitation as it was. Nothing else follows: no session
 * exists yet to end, a "password changed" mail would make no sense, and nobody is signed in — the next login
 * asks for the password just chosen.
 */
@ApplicationService
public class AccountInvitationHandler implements CheckAccountInvitationUseCase, AcceptAccountInvitationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AccountInvitationHandler.class);

    private final AccountInvitationRepository invitations;
    private final TokenHashPort hasher;
    private final UserProfilePort profiles;
    private final PasswordHasherPort passwordHasher;
    private final UserCredentialPort credentials;
    private final Clock clock;

    public AccountInvitationHandler(AccountInvitationRepository invitations, TokenHashPort hasher,
                                    UserProfilePort profiles, PasswordHasherPort passwordHasher,
                                    UserCredentialPort credentials, Clock clock) {
        this.invitations = invitations;
        this.hasher = hasher;
        this.profiles = profiles;
        this.passwordHasher = passwordHasher;
        this.credentials = credentials;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public String check(String token) {
        return accountOf(hash(token).flatMap(invitations::findByHash), clock.instant()).username();
    }

    @Override
    @Transactional
    public void accept(String token, String password) {
        UserProfile account = accountOf(hash(token).flatMap(invitations::consumeByHash), clock.instant());
        credentials.saveCredential(account.id(), passwordHasher.hash(password));
        log.info("Invitation accepted by user {}", account.id());
    }

    private Optional<String> hash(String token) {
        return token == null || token.isBlank() ? Optional.empty() : Optional.of(hasher.hash(token));
    }

    private UserProfile accountOf(Optional<AccountInvitation> invitation, Instant now) {
        return invitation
            .filter(found -> !found.isExpiredAt(now))
            .flatMap(found -> profiles.findById(found.userId()))
            .filter(UserProfile::isActive)
            .orElseThrow(AuthenticationException.InvalidInvitationToken::new);
    }
}
