package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.service.AdminGestureNotifier;
import com.nido.api.identity.domain.model.AdminResetTwoFactorCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.TwoFactorMethodsPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResetTwoFactorHandlerTest {

    @Mock UserRepository userRepository;
    @Mock TwoFactorMethodsPort twoFactorMethods;
    @Mock AdminGestureNotifier notifier;

    private AdminResetTwoFactorHandler handler;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private final Set<TwoFactorMethod> app = EnumSet.of(TwoFactorMethod.APP);

    @BeforeEach
    void setUp() {
        handler = new AdminResetTwoFactorHandler(userRepository, twoFactorMethods, notifier);
    }

    private static User user(UUID id, Role role) {
        return new User(id, "user-" + id, id + "@test.com", role, true, Instant.now(), null);
    }

    @Test
    void the_methods_ticked_are_removed_and_the_holder_told_what_remains() {
        User target = user(targetId, Role.USER);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(twoFactorMethods.removeByAdmin(targetId, app)).thenReturn(app);
        when(twoFactorMethods.activeMethods(targetId)).thenReturn(EnumSet.of(TwoFactorMethod.MAIL));

        handler.reset(new AdminResetTwoFactorCommand(targetId, callerId, Role.ADMIN, app));

        verify(notifier).twoFactorReset(target, app, EnumSet.of(TwoFactorMethod.MAIL), callerId, Role.ADMIN);
    }

    @Test
    void nothing_removed_tells_nobody() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.USER)));
        when(twoFactorMethods.removeByAdmin(targetId, app)).thenReturn(EnumSet.noneOf(TwoFactorMethod.class));

        handler.reset(new AdminResetTwoFactorCommand(targetId, callerId, Role.SUPER_ADMIN, app));

        verify(notifier, never()).twoFactorReset(any(), any(), any(), any(), any());
    }

    @Test
    void nobody_resets_their_own() {
        assertThatThrownBy(() -> handler.reset(new AdminResetTwoFactorCommand(callerId, callerId, Role.SUPER_ADMIN, app)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);
        verifyNoInteractions(userRepository, twoFactorMethods);
    }

    @Test
    void an_unknown_account_is_not_found() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.reset(new AdminResetTwoFactorCommand(targetId, callerId, Role.SUPER_ADMIN, app)))
            .isInstanceOf(IdentityException.UserNotFound.class);
        verifyNoInteractions(twoFactorMethods);
    }

    @Test
    void an_admin_cannot_reset_another_admin() {
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.ADMIN)));

        assertThatThrownBy(() -> handler.reset(new AdminResetTwoFactorCommand(targetId, callerId, Role.ADMIN, app)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);
        verifyNoInteractions(twoFactorMethods);
    }

    @Test
    void a_reset_without_a_method_is_refused() {
        assertThatThrownBy(() -> new AdminResetTwoFactorCommand(targetId, callerId, Role.ADMIN, Set.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
