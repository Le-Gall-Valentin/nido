package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.authentication.application.dto.ChangePasswordResult;
import com.synapsys.api.authentication.application.port.in.ChangePasswordUseCase;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.port.out.CredentialChangePort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CredentialChangeAdapter implements CredentialChangePort {

    private final ChangePasswordUseCase changePasswordUseCase;

    public CredentialChangeAdapter(ChangePasswordUseCase changePasswordUseCase) {
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @Override
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        switch (changePasswordUseCase.changePassword(userId, currentPassword, newPassword)) {
            case ChangePasswordResult.Success ignored -> {}
            case ChangePasswordResult.InvalidCurrentPassword ignored ->
                throw new IdentityException.InvalidCurrentPassword();
            case ChangePasswordResult.DataIntegrityError ignored ->
                throw new IdentityException.DataIntegrityError();
        }
    }
}