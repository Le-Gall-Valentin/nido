package com.nido.api.identity.application.handler;

import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.Language;
import com.nido.api.identity.domain.model.UpdateProfileCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.PasswordCheckPort;
import com.nido.api.identity.domain.port.out.ProfileMailPort;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMyProfileHandlerTest {

    @Mock UserRepository userRepository;
    @Mock UserCommandPort userCommandPort;
    @Mock PasswordCheckPort passwordCheck;
    @Mock ProfileMailPort profileMail;

    private final UUID userId = UUID.randomUUID();
    private final User jane = new User(userId, "jane", "jane@test.com", Role.USER, true, Instant.now(), Language.FR);
    private UpdateMyProfileHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateMyProfileHandler(userRepository, userCommandPort, passwordCheck, profileMail);
    }

    @Test
    void a_new_username_alone_needs_no_password_and_alerts_nobody() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        UpdateProfileCommand command = new UpdateProfileCommand(userId, "jane.doe", "jane@test.com");

        handler.updateProfile(command);

        verify(userCommandPort).updateProfile(command);
        verifyNoInteractions(passwordCheck, profileMail);
    }

    @Test
    void a_new_address_without_the_password_is_refused() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com")))
            .isInstanceOf(IdentityException.CurrentPasswordRequired.class);
        verify(userCommandPort, never()).updateProfile(any());
    }

    @Test
    void a_new_address_with_a_wrong_password_is_refused() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "wrong")).thenReturn(false);

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", "wrong")))
            .isInstanceOf(IdentityException.InvalidCurrentPassword.class);
        verify(userCommandPort, never()).updateProfile(any());
        verifyNoInteractions(profileMail);
    }

    @Test
    void a_new_address_with_the_password_is_saved_and_the_old_address_is_told() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "right")).thenReturn(true);
        UpdateProfileCommand command = new UpdateProfileCommand(userId, "jane", "new@test.com", "right");

        handler.updateProfile(command);

        verify(userCommandPort).updateProfile(command);
        verify(profileMail).emailChanged("jane", "jane@test.com", "new@test.com", Language.FR);
    }

    @Test
    void a_change_of_letter_case_only_is_not_an_address_change() {
        User mixed = new User(userId, "jane", "Jane@Test.com", Role.USER, true, Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(mixed));

        handler.updateProfile(new UpdateProfileCommand(userId, "jane", "jane@test.com"));

        verifyNoInteractions(passwordCheck, profileMail);
    }

    @Test
    void a_look_alike_address_is_an_address_change_not_a_case_change() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(
            new User(userId, "jane", "jane@gmail.com", Role.USER, true, Instant.now())));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "jane@gma\u0131l.com")))
            .isInstanceOf(IdentityException.CurrentPasswordRequired.class);
        verify(userCommandPort, never()).updateProfile(any());
        verifyNoInteractions(profileMail);
    }

    @Test
    void an_account_without_an_address_needs_the_password_to_set_one() {
        User noAddress = new User(userId, "jane", null, Role.USER, true, Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(noAddress));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com")))
            .isInstanceOf(IdentityException.CurrentPasswordRequired.class);
        verify(userCommandPort, never()).updateProfile(any());
    }

    @Test
    void a_deactivated_account_changes_nothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(
            new User(userId, "jane", "jane@test.com", Role.USER, false, Instant.now())));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "x", "x@test.com", "right")))
            .isInstanceOf(IdentityException.UserNotActive.class);
        verifyNoInteractions(passwordCheck, profileMail);
    }

    @Test
    void an_unknown_account_changes_nothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "x", "x@test.com")))
            .isInstanceOf(IdentityException.UserNotFound.class);
    }
}
