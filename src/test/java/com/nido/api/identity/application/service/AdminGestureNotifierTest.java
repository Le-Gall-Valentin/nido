package com.nido.api.identity.application.service;

import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import com.nido.api.identity.domain.port.out.AdminAccountMailPort;
import com.nido.api.identity.domain.port.out.AdminActivityNotificationPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminGestureNotifierTest {

    @Mock UserRepository users;
    @Mock AccountInvitationPort invitations;
    @Mock AdminAccountMailPort mails;
    @Mock AdminActivityNotificationPort activity;

    private final User bob = user("bob", Role.ADMIN);
    private final User root = user("root", Role.SUPER_ADMIN);
    private final User alice = user("alice", Role.SUPER_ADMIN);
    private final User carol = user("carol", Role.USER);
    private AdminGestureNotifier notifier;

    private static User user(String name, Role role) {
        return new User(UUID.randomUUID(), name, name + "@test.com", role, true, Instant.now(), null);
    }

    @BeforeEach
    void setUp() {
        notifier = new AdminGestureNotifier(users, invitations, mails, activity);
        when(users.findById(bob.id())).thenReturn(Optional.of(bob));
        when(users.findById(root.id())).thenReturn(Optional.of(root));
        when(users.findActiveByRole(Role.SUPER_ADMIN)).thenReturn(List.of(alice, root));
        when(invitations.isInvited(carol.id())).thenReturn(false);
    }

    @Test
    void an_admin_deactivating_an_account_tells_its_holder_and_every_super_administrator() {
        notifier.deactivated(carol, bob.id(), Role.ADMIN);

        verify(mails).deactivated(carol, "bob");
        verify(activity).accountDeactivated(List.of(alice, root), "bob", "carol");
    }

    @Test
    void a_super_administrator_s_gesture_is_told_to_the_holder_only() {
        notifier.deactivated(carol, root.id(), Role.SUPER_ADMIN);

        verify(mails).deactivated(carol, "root");
        verifyNoInteractions(activity);
    }

    @Test
    void an_invited_account_hears_nothing_but_the_super_administrators_still_do() {
        when(invitations.isInvited(carol.id())).thenReturn(true);

        notifier.deactivated(carol, bob.id(), Role.ADMIN);
        notifier.reactivated(carol, bob.id(), Role.ADMIN);
        notifier.roleChanged(carol, Role.ADMIN, root.id());

        verifyNoInteractions(mails);
        verify(activity).accountDeactivated(List.of(alice, root), "bob", "carol");
        verify(activity).accountReactivated(List.of(alice, root), "bob", "carol");
    }

    @Test
    void a_role_change_is_told_to_the_holder_only() {
        notifier.roleChanged(carol, Role.ADMIN, root.id());

        verify(mails).roleChanged(carol, "root", Role.ADMIN);
        verifyNoInteractions(activity);
    }

    @Test
    void a_reactivation_and_a_reset_second_factor_are_told_to_both() {
        notifier.reactivated(carol, bob.id(), Role.ADMIN);
        notifier.totpReset(carol, bob.id(), Role.ADMIN);

        verify(mails).reactivated(carol, "bob");
        verify(mails).totpReset(carol, "bob");
        verify(activity).accountReactivated(List.of(alice, root), "bob", "carol");
        verify(activity).totpReset(List.of(alice, root), "bob", "carol");
    }

    @Test
    void a_creation_is_told_to_the_super_administrators_only_the_invitation_being_its_own_mail() {
        notifier.accountCreated(carol, bob.id(), Role.ADMIN);

        verify(activity).accountCreated(List.of(alice, root), "bob", "carol");
        verifyNoInteractions(mails);
    }

    @Test
    void deleting_an_account_that_joined_says_goodbye() {
        notifier.deleted(carol, false, bob.id(), Role.ADMIN);

        verify(mails).deleted(carol, "bob");
        verify(mails, never()).invitationCancelled(any(), any());
        verify(activity).accountDeleted(List.of(alice, root), "bob", "carol", false);
    }

    @Test
    void deleting_an_invited_account_cancels_its_invitation() {
        notifier.deleted(carol, true, root.id(), Role.SUPER_ADMIN);

        verify(mails).invitationCancelled(carol, "root");
        verify(mails, never()).deleted(any(), any());
        verifyNoInteractions(activity);
    }

    @Test
    void without_an_active_super_administrator_nobody_else_is_told() {
        when(users.findActiveByRole(Role.SUPER_ADMIN)).thenReturn(List.of());

        notifier.deactivated(carol, bob.id(), Role.ADMIN);

        verify(mails).deactivated(carol, "bob");
        verifyNoInteractions(activity);
    }

    @Test
    void an_administrator_who_cannot_be_named_tells_nobody() {
        UUID unknown = UUID.randomUUID();
        when(users.findById(unknown)).thenReturn(Optional.empty());

        notifier.deactivated(carol, unknown, Role.ADMIN);

        verifyNoInteractions(mails, activity);
    }

    @Test
    void an_administrator_deleting_an_invited_account_is_told_as_a_cancelled_invitation() {
        notifier.deleted(carol, true, bob.id(), Role.ADMIN);

        verify(mails).invitationCancelled(carol, "bob");
        verify(activity).accountDeleted(List.of(alice, root), "bob", "carol", true);
    }
}
