package com.nido.api.identity.application.handler;

import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.model.UserSelfView;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserHandlerTest {

    @Mock UserRepository userRepository;
    @Mock TwoFactorMethodsPort twoFactorMethods;

    private GetCurrentUserHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetCurrentUserHandler(userRepository, twoFactorMethods);
    }

    @Test
    void getCurrentUser_found_totpDisabled_returnsView() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        User user = new User(id, "user1", "u@test.com", Role.USER, true, now, null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(twoFactorMethods.activeMethods(id)).thenReturn(EnumSet.noneOf(TwoFactorMethod.class));

        UserSelfView view = handler.getCurrentUser(id);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.username()).isEqualTo("user1");
        assertThat(view.email()).isEqualTo("u@test.com");
        assertThat(view.role()).isEqualTo(Role.USER);
        assertThat(view.createdAt()).isEqualTo(now);
        assertThat(view.twoFactorMethods()).isEmpty();
    }

    @Test
    void getCurrentUser_found_withTheAppOn_returnsViewWithTheApp() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "user1", "u@test.com", Role.USER, true, Instant.now(), null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(twoFactorMethods.activeMethods(id)).thenReturn(EnumSet.of(TwoFactorMethod.APP));

        UserSelfView view = handler.getCurrentUser(id);

        assertThat(view.twoFactorMethods()).containsExactly(TwoFactorMethod.APP);
    }

    @Test
    void getCurrentUser_notFound_throwsUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.getCurrentUser(id))
            .isInstanceOf(IdentityException.UserNotFound.class);
    }

    @Test
    void getCurrentUser_inactiveUser_throwsUserNotActive() {
        UUID id = UUID.randomUUID();
        User inactive = new User(id, "user1", "u@test.com", Role.USER, false, Instant.now(), null);
        when(userRepository.findById(id)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> handler.getCurrentUser(id))
            .isInstanceOf(IdentityException.UserNotActive.class);
    }
}