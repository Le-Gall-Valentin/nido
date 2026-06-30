package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.model.UserSelfView;
import com.synapsys.api.identity.domain.port.out.TotpStatusPort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserHandlerTest {

    @Mock UserRepository userRepository;
    @Mock TotpStatusPort totpStatusPort;

    private GetCurrentUserHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetCurrentUserHandler(userRepository, totpStatusPort);
    }

    @Test
    void getCurrentUser_found_totpDisabled_returnsView() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        User user = new User(id, "user1", "u@test.com", Role.USER, true, now);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(totpStatusPort.isTotpEnabled(id)).thenReturn(false);

        UserSelfView view = handler.getCurrentUser(id);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.username()).isEqualTo("user1");
        assertThat(view.email()).isEqualTo("u@test.com");
        assertThat(view.role()).isEqualTo(Role.USER);
        assertThat(view.createdAt()).isEqualTo(now);
        assertThat(view.totpEnabled()).isFalse();
    }

    @Test
    void getCurrentUser_found_totpEnabled_returnsViewWithTotpTrue() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "user1", "u@test.com", Role.USER, true, Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(totpStatusPort.isTotpEnabled(id)).thenReturn(true);

        UserSelfView view = handler.getCurrentUser(id);

        assertThat(view.totpEnabled()).isTrue();
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
        User inactive = new User(id, "user1", "u@test.com", Role.USER, false, Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> handler.getCurrentUser(id))
            .isInstanceOf(IdentityException.UserNotActive.class);
    }
}