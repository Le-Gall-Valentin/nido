package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.service.AdminGestureNotifier;
import com.nido.api.identity.domain.model.AdminResetTotpCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.MfaAdminResetTotpPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.model.Role;
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
    @Mock AdminGestureNotifier notifier;

    private AdminResetTotpHandler handler;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new AdminResetTotpHandler(userRepository, mfaResetTotp, notifier);
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
        return new User(id, "user-" + id, id + "@test.com", role, true, Instant.now(), null);
    }

    @Test
    void reset_tells_only_when_a_second_factor_was_actually_on() {
        User target = user(targetId, Role.USER);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(mfaResetTotp.disableTotpIfEnabled(targetId)).thenReturn(true).thenReturn(false);

        handler.reset(new AdminResetTotpCommand(targetId, callerId, Role.ADMIN));
        handler.reset(new AdminResetTotpCommand(targetId, callerId, Role.ADMIN));

        verify(notifier, times(1)).totpReset(target, callerId, Role.ADMIN);
    }
}
