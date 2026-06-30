package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.AdminResetTotpCommand;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.MfaAdminResetTotpPort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminResetTotpHandlerTest {

    @Mock UserRepository userRepository;
    @Mock MfaAdminResetTotpPort mfaResetTotp;

    private AdminResetTotpHandler handler;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new AdminResetTotpHandler(userRepository, mfaResetTotp);
    }

    @Test
    void reset_superAdminResetsUser_succeeds() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.USER)));

        assertThatCode(() -> handler.reset(new AdminResetTotpCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .doesNotThrowAnyException();

        verify(mfaResetTotp).disableTotpIfEnabled(targetId);
    }

    @Test
    void reset_selfReset_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.reset(new AdminResetTotpCommand(callerId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(mfaResetTotp);
    }

    @Test
    void reset_targetNotFound_throwsUserNotFound() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.reset(new AdminResetTotpCommand(targetId, callerId, Role.SUPER_ADMIN)))
            .isInstanceOf(IdentityException.UserNotFound.class);

        verifyNoInteractions(mfaResetTotp);
    }

    @Test
    void reset_insufficientRole_throwsInsufficientPermissions() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.ADMIN)));

        assertThatThrownBy(() -> handler.reset(new AdminResetTotpCommand(targetId, callerId, Role.ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);

        verifyNoInteractions(mfaResetTotp);
    }

    private User user(UUID id, Role role) {
        return new User(id, "user-" + id, id + "@test.com", role, true, Instant.now());
    }
}