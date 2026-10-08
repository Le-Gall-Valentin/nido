package com.nido.api.identity.application.handler;

import com.nido.api.identity.domain.model.EmailAddress;
import com.nido.api.identity.domain.model.EmailCodeCheck;
import com.nido.api.identity.domain.model.EmailCodeDelivery;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.ProfileUpdate;
import com.nido.api.identity.domain.model.UpdateProfileCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountRecoveryPort;
import com.nido.api.identity.domain.port.out.AddressChangeCodePort;
import com.nido.api.identity.domain.port.out.PasswordCheckPort;
import com.nido.api.identity.domain.port.out.ProfileMailPort;
import com.nido.api.identity.domain.port.out.UserCommandPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.model.Language;
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
import static org.mockito.Mockito.inOrder;
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
    @Mock AccountRecoveryPort accountRecovery;
    @Mock AddressChangeCodePort addressChangeCode;

    private final UUID userId = UUID.randomUUID();
    private final User jane = new User(userId, "jane", "jane@test.com", Role.USER, true, Instant.now(), Language.FR);
    private UpdateMyProfileHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateMyProfileHandler(userRepository, userCommandPort, passwordCheck, profileMail, accountRecovery,
            addressChangeCode);
    }

    @Test
    void a_new_username_alone_needs_no_password_and_alerts_nobody() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        UpdateProfileCommand command = new UpdateProfileCommand(userId, "jane.doe", "jane@test.com", null);

        handler.updateProfile(command);

        verify(userCommandPort).updateProfile(command);
        verifyNoInteractions(passwordCheck, profileMail);
    }

    @Test
    void a_new_address_without_the_password_is_refused() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", null)))
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
    void a_new_address_voids_every_reset_link_the_old_one_received() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "right")).thenReturn(true);

        handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", "right"));

        verify(accountRecovery).forgetResetLinks(userId);
    }

    @Test
    void a_rename_and_an_address_change_in_one_save_greets_the_old_address_by_the_previous_username() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "right")).thenReturn(true);

        handler.updateProfile(new UpdateProfileCommand(userId, "someone.else", "new@test.com", "right"));

        verify(profileMail).emailChanged("jane", "jane@test.com", "new@test.com", Language.FR);
    }

    @Test
    void a_change_of_letter_case_only_is_not_an_address_change() {
        User mixed = new User(userId, "jane", "Jane@Test.com", Role.USER, true, Instant.now(), null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mixed));

        handler.updateProfile(new UpdateProfileCommand(userId, "jane", "jane@test.com", null));

        verifyNoInteractions(passwordCheck, profileMail, accountRecovery);
    }

    @Test
    void a_look_alike_address_is_an_address_change_not_a_case_change() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(
            new User(userId, "jane", "jane@gmail.com", Role.USER, true, Instant.now(), null)));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "jane@gma\u0131l.com", null)))
            .isInstanceOf(IdentityException.CurrentPasswordRequired.class);
        verify(userCommandPort, never()).updateProfile(any());
        verifyNoInteractions(profileMail);
    }

    @Test
    void an_account_without_an_address_needs_the_password_to_set_one() {
        User noAddress = new User(userId, "jane", null, Role.USER, true, Instant.now(), null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(noAddress));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", null)))
            .isInstanceOf(IdentityException.CurrentPasswordRequired.class);
        verify(userCommandPort, never()).updateProfile(any());
    }

    @Test
    void a_deactivated_account_changes_nothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(
            new User(userId, "jane", "jane@test.com", Role.USER, false, Instant.now(), null)));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "xyz", "x@test.com", "right")))
            .isInstanceOf(IdentityException.UserNotActive.class);
        verifyNoInteractions(passwordCheck, profileMail);
    }

    @Test
    void an_unknown_account_changes_nothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "xyz", "x@test.com", null)))
            .isInstanceOf(IdentityException.UserNotFound.class);
    }

    @Test
    void with_the_mail_method_on_a_new_address_first_gets_a_code_and_nothing_is_saved() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(true);
        when(userRepository.findByEmail(new EmailAddress("new@test.com"))).thenReturn(Optional.empty());
        when(addressChangeCode.send(userId, "new@test.com")).thenReturn(new EmailCodeDelivery.Sent(60));

        ProfileUpdate update = handler.updateProfile(new UpdateProfileCommand(userId, "jane", "New@Test.com", "password"));

        assertThat(update).isEqualTo(new ProfileUpdate.EmailCodeSent("new@test.com", 60));
        verify(userCommandPort, never()).updateProfile(any());
        verifyNoInteractions(profileMail, accountRecovery);
    }

    @Test
    void the_right_code_saves_the_address_and_tells_the_old_one() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(true);
        when(userRepository.findByEmail(new EmailAddress("new@test.com"))).thenReturn(Optional.empty());
        when(addressChangeCode.check(userId, "new@test.com", "004213")).thenReturn(EmailCodeCheck.VALID);
        UpdateProfileCommand command = new UpdateProfileCommand(userId, "jane", "new@test.com", "password", "004213");

        assertThat(handler.updateProfile(command)).isEqualTo(new ProfileUpdate.Saved());
        verify(userCommandPort).updateProfile(command);
        verify(profileMail).emailChanged("jane", "jane@test.com", "new@test.com", Language.FR);
    }

    @Test
    void a_wrong_code_saves_nothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(true);
        when(userRepository.findByEmail(new EmailAddress("new@test.com"))).thenReturn(Optional.empty());
        when(addressChangeCode.check(userId, "new@test.com", "000000")).thenReturn(EmailCodeCheck.INVALID);

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", "password", "000000")))
            .isInstanceOf(IdentityException.EmailCodeInvalid.class);
        verify(userCommandPort, never()).updateProfile(any());
    }

    @Test
    void an_address_another_account_uses_is_refused_before_any_code_leaves() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(true);
        User john = new User(UUID.randomUUID(), "john", "john@test.com", Role.USER, true, Instant.now(), null);
        when(userRepository.findByEmail(new EmailAddress("john@test.com"))).thenReturn(Optional.of(john));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "john@test.com", "password")))
            .isInstanceOf(IdentityException.EmailAlreadyExists.class);
        verify(addressChangeCode, never()).send(any(), any());
    }

    @Test
    void the_password_is_checked_before_a_code_is_sent() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "wrong")).thenReturn(false);

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", "wrong")))
            .isInstanceOf(IdentityException.InvalidCurrentPassword.class);
        verifyNoInteractions(addressChangeCode);
    }

    @Test
    void without_the_mail_method_the_address_is_saved_as_before() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(false);
        UpdateProfileCommand command = new UpdateProfileCommand(userId, "jane", "new@test.com", "password");

        assertThat(handler.updateProfile(command)).isEqualTo(new ProfileUpdate.Saved());
        verify(userCommandPort).updateProfile(command);
        verify(addressChangeCode, never()).send(any(), any());
        verify(addressChangeCode, never()).forgoMailMethod(any());
    }

    @Test
    void with_mail_off_the_address_is_saved_and_the_mail_method_goes_since_nothing_proves_the_address() {
        // Kept, the method would send every future code to an address nobody proved: a typo would lock the holder
        // out once mail is back, and whoever changed it would get the codes.
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(true);
        when(userRepository.findByEmail(new EmailAddress("new@test.com"))).thenReturn(Optional.empty());
        when(addressChangeCode.send(userId, "new@test.com")).thenReturn(new EmailCodeDelivery.Unavailable());
        UpdateProfileCommand command = new UpdateProfileCommand(userId, "jane", "new@test.com", "password");

        assertThat(handler.updateProfile(command)).isEqualTo(new ProfileUpdate.SavedMailMethodRemoved());
        var order = inOrder(userCommandPort, addressChangeCode);
        order.verify(userCommandPort).updateProfile(command);
        order.verify(addressChangeCode).forgoMailMethod(userId);
        verify(profileMail).emailChanged("jane", "jane@test.com", "new@test.com", Language.FR);
    }

    @Test
    void saving_again_within_the_minute_says_when_a_code_can_leave() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(true);
        when(userRepository.findByEmail(new EmailAddress("new@test.com"))).thenReturn(Optional.empty());
        when(addressChangeCode.send(userId, "new@test.com")).thenReturn(new EmailCodeDelivery.TooSoon(40));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", "password")))
            .isInstanceOfSatisfying(IdentityException.EmailCodeResendTooSoon.class, e -> assertThat(e.seconds()).isEqualTo(40));
        verify(userCommandPort, never()).updateProfile(any());
    }

    @Test
    void the_account_send_limit_says_when_a_code_can_leave() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(true);
        when(userRepository.findByEmail(new EmailAddress("new@test.com"))).thenReturn(Optional.empty());
        when(addressChangeCode.send(userId, "new@test.com")).thenReturn(new EmailCodeDelivery.LimitReached(420));

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", "password")))
            .isInstanceOfSatisfying(IdentityException.EmailCodeSendLimitReached.class, e -> assertThat(e.seconds()).isEqualTo(420));
        verify(userCommandPort, never()).updateProfile(any());
    }

    @Test
    void a_letter_case_change_asks_for_no_code() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        UpdateProfileCommand command = new UpdateProfileCommand(userId, "jane", "Jane@Test.com", null);

        assertThat(handler.updateProfile(command)).isEqualTo(new ProfileUpdate.Saved());
        verifyNoInteractions(addressChangeCode);
    }

    @Test
    void a_code_spent_by_wrong_guesses_says_to_ask_for_another() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(true);
        when(userRepository.findByEmail(new EmailAddress("new@test.com"))).thenReturn(Optional.empty());
        when(addressChangeCode.check(userId, "new@test.com", "000000")).thenReturn(EmailCodeCheck.SPENT);

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", "password", "000000")))
            .isInstanceOf(IdentityException.EmailCodeSpent.class);
        verify(userCommandPort, never()).updateProfile(any());
    }

    @Test
    void a_code_that_expired_says_so_rather_than_blame_wrong_guesses() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(jane));
        when(passwordCheck.matches(userId, "password")).thenReturn(true);
        when(addressChangeCode.mailMethodOn(userId)).thenReturn(true);
        when(userRepository.findByEmail(new EmailAddress("new@test.com"))).thenReturn(Optional.empty());
        when(addressChangeCode.check(userId, "new@test.com", "004213")).thenReturn(EmailCodeCheck.EXPIRED);

        assertThatThrownBy(() -> handler.updateProfile(new UpdateProfileCommand(userId, "jane", "new@test.com", "password", "004213")))
            .isInstanceOf(IdentityException.EmailCodeExpired.class);
        verify(userCommandPort, never()).updateProfile(any());
    }
}
