package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.authentication.application.dto.ChangePasswordResult;
import com.synapsys.api.authentication.application.port.in.ChangePasswordUseCase;
import com.synapsys.api.identity.domain.model.IdentityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialChangeAdapterTest {

    @Mock ChangePasswordUseCase changePasswordUseCase;

    @Test
    void changePassword_success_doesNotThrow() {
        CredentialChangeAdapter adapter = new CredentialChangeAdapter(changePasswordUseCase);
        UUID userId = UUID.randomUUID();
        when(changePasswordUseCase.changePassword(any(), any(), any()))
            .thenReturn(new ChangePasswordResult.Success());

        assertThatCode(() -> adapter.changePassword(userId, "current", "newPass"))
            .doesNotThrowAnyException();
    }

    @Test
    void changePassword_invalidCurrentPassword_throwsIdentityException() {
        CredentialChangeAdapter adapter = new CredentialChangeAdapter(changePasswordUseCase);
        UUID userId = UUID.randomUUID();
        when(changePasswordUseCase.changePassword(any(), any(), any()))
            .thenReturn(new ChangePasswordResult.InvalidCurrentPassword());

        assertThatThrownBy(() -> adapter.changePassword(userId, "wrong", "newPass"))
            .isInstanceOf(IdentityException.InvalidCurrentPassword.class);
    }

    @Test
    void changePassword_dataIntegrityError_throwsIdentityException() {
        CredentialChangeAdapter adapter = new CredentialChangeAdapter(changePasswordUseCase);
        UUID userId = UUID.randomUUID();
        when(changePasswordUseCase.changePassword(any(), any(), any()))
            .thenReturn(new ChangePasswordResult.DataIntegrityError());

        assertThatThrownBy(() -> adapter.changePassword(userId, "current", "newPass"))
            .isInstanceOf(IdentityException.DataIntegrityError.class);
    }
}