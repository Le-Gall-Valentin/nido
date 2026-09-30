package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.ChangePasswordResult;
import com.nido.api.authentication.application.port.in.ChangePasswordUseCase;
import com.nido.api.authentication.application.service.PasswordChangeConsequences;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.PasswordHasherPort;
import com.nido.api.authentication.domain.port.out.PasswordVerifierPort;
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
    private final PasswordChangeConsequences consequences;

    public ChangePasswordHandler(UserCredentialsPort userCredentialsPort,
                                 PasswordVerifierPort passwordVerifier,
                                 PasswordHasherPort passwordHasher,
                                 UserCredentialPort userCredentialPort,
                                 PasswordChangeConsequences consequences) {
        this.userCredentialsPort = userCredentialsPort;
        this.passwordVerifier = passwordVerifier;
        this.passwordHasher = passwordHasher;
        this.userCredentialPort = userCredentialPort;
        this.consequences = consequences;
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
        // Every way in the old password opened is closed — the caller's own session included, which is
        // why the frontend signs out right after — and the holder is told.
        consequences.apply(AccountContact.of(creds));
        log.info("Password changed for user {} — all sessions ended", userId);
        return new ChangePasswordResult.Success();
    }
}