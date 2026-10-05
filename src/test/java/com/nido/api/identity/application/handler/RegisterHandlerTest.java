package com.nido.api.identity.application.handler;

import com.nido.api.identity.application.service.AdminGestureNotifier;
import com.nido.api.identity.domain.model.CreateUserProfileCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.InvitationDelivery;
import com.nido.api.identity.domain.model.RegisteredAccount;
import com.nido.api.identity.domain.model.RegisterCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import com.nido.api.identity.domain.port.out.PersonalSpaceInitPort;
import com.nido.api.identity.domain.port.out.TotpRecordInitPort;
import com.nido.api.identity.domain.port.out.UserCommandPort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterHandlerTest {

    @Mock UserCommandPort userCommandPort;
    @Mock UserRepository userRepository;
    @Mock AccountInvitationPort invitations;
    @Mock AdminGestureNotifier notifier;
    @Mock TotpRecordInitPort totpRecordInitPort;
    @Mock PersonalSpaceInitPort personalSpaceInitPort;

    private RegisterHandler handler;

    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new RegisterHandler(userCommandPort, userRepository, invitations, totpRecordInitPort, personalSpaceInitPort, notifier);
        lenient().when(userRepository.findById(callerId))
            .thenReturn(Optional.of(new User(callerId, "root", "root@test.com", Role.SUPER_ADMIN, true, Instant.now(), null)));
        lenient().when(invitations.invite(any(), any())).thenReturn(new InvitationDelivery.Mailed());
    }

    @Test
    void register_superAdmin_can_create_admin() {
        User created = user(UUID.randomUUID(), "newadmin", Role.ADMIN);
        when(userCommandPort.createProfile(any(CreateUserProfileCommand.class))).thenReturn(created);

        User result = handler.register(cmd("newadmin", Role.ADMIN), callerId, Role.SUPER_ADMIN).user();

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        verify(invitations).invite(created.id(), "root");
        verify(totpRecordInitPort).initForUser(created.id());
    }

    @Test
    void register_superAdmin_can_create_user() {
        User created = user(UUID.randomUUID(), "newuser", Role.USER);
        when(userCommandPort.createProfile(any())).thenReturn(created);

        User result = handler.register(cmd("newuser", Role.USER), callerId, Role.SUPER_ADMIN).user();

        assertThat(result.role()).isEqualTo(Role.USER);
    }

    @Test
    void register_insufficientPermissions_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.register(cmd("sa-account", Role.SUPER_ADMIN), callerId, Role.SUPER_ADMIN))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);
        verifyNoInteractions(userCommandPort);
    }

    @Test
    void register_userCannotCreateAnyone_throwsInsufficientPermissions() {
        assertThatThrownBy(() -> handler.register(cmd("someone", Role.USER), callerId, Role.USER))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);
        verifyNoInteractions(userCommandPort);
    }

    @Test
    void register_creates_the_personal_space() {
        User created = user(UUID.randomUUID(), "newuser", Role.USER);
        when(userCommandPort.createProfile(any(CreateUserProfileCommand.class))).thenReturn(created);

        handler.register(cmd("newuser", Role.USER), callerId, Role.ADMIN);

        verify(personalSpaceInitPort).initForUser(created.id());
    }

    @Test
    void register_admin_can_create_user() {
        User created = user(UUID.randomUUID(), "newuser", Role.USER);
        when(userCommandPort.createProfile(any(CreateUserProfileCommand.class))).thenReturn(created);

        User result = handler.register(cmd("newuser", Role.USER), callerId, Role.ADMIN).user();

        assertThat(result.role()).isEqualTo(Role.USER);
        verify(invitations).invite(created.id(), "root");
        verify(notifier).accountCreated(created, callerId, Role.ADMIN);
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExists() {
        when(userCommandPort.createProfile(any())).thenThrow(new IdentityException.EmailAlreadyExists());

        assertThatThrownBy(() -> handler.register(cmd("new", Role.USER), callerId, Role.SUPER_ADMIN))
            .isInstanceOf(IdentityException.EmailAlreadyExists.class);
    }

    @Test
    void register_totpInitFails_propagatesException() {
        User created = user(UUID.randomUUID(), "newuser", Role.USER);
        when(userCommandPort.createProfile(any())).thenReturn(created);
        doThrow(new RuntimeException("totp store unavailable"))
            .when(totpRecordInitPort).initForUser(created.id());

        assertThatThrownBy(() -> handler.register(cmd("newuser", Role.USER), callerId, Role.SUPER_ADMIN))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("totp store unavailable");
    }

    @Test
    void register_invitationFails_propagatesException() {
        User created = user(UUID.randomUUID(), "newuser", Role.USER);
        when(userCommandPort.createProfile(any())).thenReturn(created);
        when(invitations.invite(created.id(), "root")).thenThrow(new RuntimeException("invitation store unavailable"));

        assertThatThrownBy(() -> handler.register(cmd("newuser", Role.USER), callerId, Role.SUPER_ADMIN))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("invitation store unavailable");
    }

    @Test
    void register_hands_back_how_the_invitation_left() {
        User created = user(UUID.randomUUID(), "newuser", Role.USER);
        when(userCommandPort.createProfile(any())).thenReturn(created);
        when(invitations.invite(created.id(), "root")).thenReturn(new InvitationDelivery.Link("/welcome#token=x"));

        RegisteredAccount registered = handler.register(cmd("newuser", Role.USER), callerId, Role.ADMIN);

        assertThat(registered.user()).isEqualTo(created);
        assertThat(registered.invitation()).isEqualTo(new InvitationDelivery.Link("/welcome#token=x"));
    }

    @Test
    void register_duplicateUsername_throwsUsernameAlreadyExists() {
        when(userCommandPort.createProfile(any())).thenThrow(new IdentityException.UsernameAlreadyExists());

        assertThatThrownBy(() -> handler.register(cmd("existing", Role.USER), callerId, Role.SUPER_ADMIN))
            .isInstanceOf(IdentityException.UsernameAlreadyExists.class);
    }

    private RegisterCommand cmd(String username, Role role) {
        return new RegisterCommand(username, username + "@test.com", role);
    }

    private User user(UUID id, String username, Role role) {
        return new User(id, username, username + "@test.com", role, true, Instant.now(), null);
    }

    @Test
    void register_by_a_caller_whose_account_cannot_be_found_creates_nothing() {
        when(userRepository.findById(callerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.register(new RegisterCommand("carol", "carol@test.com", Role.USER), callerId, Role.SUPER_ADMIN))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);
        verify(userCommandPort, never()).createProfile(any());
        verify(invitations, never()).invite(any(), any());
    }
}