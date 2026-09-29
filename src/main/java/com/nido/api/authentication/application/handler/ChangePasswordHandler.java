package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.ChangePasswordResult;
import com.nido.api.authentication.application.port.in.ChangePasswordUseCase;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.authentication.domain.port.out.IssuedTokenCutoffPort;
import com.nido.api.authentication.domain.port.out.PasswordHasherPort;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.PasswordVerifierPort;
import com.nido.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.nido.api.authentication.domain.port.out.UserCredentialPort;
import com.nido.api.authentication.domain.port.out.UserCredentialsPort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class ChangePasswordHandler implements ChangePasswordUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordHandler.class);

    private final UserCredentialsPort userCredentialsPort;
    private final PasswordVerifierPort passwordVerifier;
    private final PasswordHasherPort passwordHasher;
    private final UserCredentialPort userCredentialPort;
    private final RefreshTokenRevocationPort refreshTokenRevocationPort;
    private final PasswordResetTokenRepository resetTokens;
    private final IssuedTokenCutoffPort cutoff;
    private final AccountMailPort accountMail;

    public ChangePasswordHandler(UserCredentialsPort userCredentialsPort,
                                 PasswordVerifierPort passwordVerifier,
                                 PasswordHasherPort passwordHasher,
                                 UserCredentialPort userCredentialPort,
                                 RefreshTokenRevocationPort refreshTokenRevocationPort,
                                 PasswordResetTokenRepository resetTokens,
                                 IssuedTokenCutoffPort cutoff,
                                 AccountMailPort accountMail) {
        this.userCredentialsPort = userCredentialsPort;
        this.passwordVerifier = passwordVerifier;
        this.passwordHasher = passwordHasher;
        this.userCredentialPort = userCredentialPort;
        this.refreshTokenRevocationPort = refreshTokenRevocationPort;
        this.resetTokens = resetTokens;
        this.cutoff = cutoff;
        this.accountMail = accountMail;
    }

    @Override
    @Transactional
    public ChangePasswordResult changePassword(UUID userId, String currentPassword, String newPassword) {
        UserCredentials creds = userCredentialsPort.findById(userId).orElse(null);
        if (creds == null) {
            log.error("Data integrity: no credentials found for authenticated user {}", userId);
            return new ChangePasswordResult.DataIntegrityError();
        }
        if (!passwordVerifier.matches(currentPassword, creds.passwordHash())) {
            return new ChangePasswordResult.InvalidCurrentPassword();
        }
        String newHash = passwordHasher.hash(newPassword);
        userCredentialPort.updatePasswordHash(userId, newHash);
        // Changing a password is how someone reacts to a suspected compromise, so every
        // refresh token issued under the old one has to stop working — otherwise a stolen
        // token keeps minting access tokens for its full 30-day life and the change achieves
        // nothing against the case it exists for.
        //
        // revokeAllForUser commits in its own transaction (REQUIRES_NEW, see
        // RefreshTokenRepositoryAdapter). Should this transaction fail at commit after the
        // call, the tokens stay revoked while the password stays unchanged: the user signs in
        // again with the old password. That is the harmless direction of the two.
        refreshTokenRevocationPort.revokeAllForUser(userId);
        // Since mail and password resets exist, a change here ends exactly like a reset: no older link
        // may outlive a deliberate change, access tokens already issued stop now — the caller's own
        // included, which is why the frontend signs out right after — and the holder is told, in case
        // it was not them.
        resetTokens.deleteAllForUser(userId);
        cutoff.cutOffNow(userId);
        accountMail.passwordChanged(AccountContact.of(creds));
        log.info("Password changed for user {} — all sessions ended", userId);
        return new ChangePasswordResult.Success();
    }
}