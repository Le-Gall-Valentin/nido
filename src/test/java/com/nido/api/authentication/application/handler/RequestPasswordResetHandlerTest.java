package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.service.InvitationIssuer;
import com.nido.api.authentication.domain.model.AccountInvitation;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.PasswordResetRules;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.AccountLockPort;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.ResetTokenGeneratorPort;
import com.nido.api.authentication.domain.port.out.TokenHashPort;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestPasswordResetHandlerTest {

    @Mock UserProfilePort profiles;
    @Mock PasswordResetTokenRepository tokens;
    @Mock AccountInvitationRepository invitations;
    @Mock InvitationIssuer issuer;
    @Mock ResetTokenGeneratorPort generator;
    @Mock TokenHashPort hasher;
    @Mock AccountMailPort mail;
    @Mock AccountLockPort accountLock;

    private final Instant now = Instant.parse("2026-09-28T10:00:00Z");
    private final UserProfile jane = new UserProfile(UUID.randomUUID(), "jane", "jane@test.com", true, Role.USER,
        Instant.parse("2026-01-01T00:00:00Z"), Language.FR);
    private RequestPasswordResetHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RequestPasswordResetHandler(profiles, tokens, invitations, issuer, generator, hasher, mail, accountLock, Clock.fixed(now, ZoneOffset.UTC));
        when(profiles.findByIdentifier(any())).thenReturn(Optional.empty());
        when(tokens.latestIssuedAt(any())).thenReturn(Optional.empty());
        when(invitations.findByUserId(any())).thenReturn(Optional.empty());
        when(generator.newToken()).thenReturn("RAW");
        when(hasher.hash("RAW")).thenReturn("HASH");
    }

    @Test
    void a_known_username_gets_a_link_whose_hash_alone_is_stored() {
        when(profiles.findByIdentifier("jane")).thenReturn(Optional.of(jane));

        handler.request("  jane ");

        InOrder order = inOrder(tokens, mail);
        order.verify(tokens).deleteAllForUser(jane.id());
        order.verify(tokens).save(jane.id(), "HASH", now, now.plus(PasswordResetRules.VALIDITY));
        order.verify(mail).passwordResetRequested(AccountContact.of(jane), "RAW", now.plus(PasswordResetRules.VALIDITY),
            Duration.ofMinutes(30));
    }

    @Test
    void an_invited_account_gets_a_new_invitation_instead_of_a_reset_link() {
        when(profiles.findByIdentifier("jane")).thenReturn(Optional.of(jane));
        when(invitations.findByUserId(jane.id()))
            .thenReturn(Optional.of(new AccountInvitation(jane.id(), now.minus(Duration.ofDays(8)), now.minus(Duration.ofDays(1)))));

        handler.request("jane");

        verify(issuer).issue(AccountContact.of(jane), null);
        verify(tokens, never()).save(any(), any(), any(), any());
        verify(mail, never()).passwordResetRequested(any(), any(), any(), any());
    }

    @Test
    void an_invitation_sent_a_moment_ago_is_not_sent_again() {
        when(profiles.findByIdentifier("jane")).thenReturn(Optional.of(jane));
        when(invitations.findByUserId(jane.id()))
            .thenReturn(Optional.of(new AccountInvitation(jane.id(), now.minusSeconds(60), now.plus(Duration.ofDays(7)))));

        handler.request("jane");

        verifyNoInteractions(issuer);
        verify(tokens, never()).save(any(), any(), any(), any());
    }

    @Test
    void an_address_works_too_whatever_its_letter_case() {
        when(profiles.findByIdentifier("JANE@test.com")).thenReturn(Optional.of(jane));

        handler.request("JANE@test.com");

        verify(mail).passwordResetRequested(any(), any(), any(), any());
    }

    @Test
    void nobody_behind_the_identifier_sends_nothing() {
        handler.request("nobody");

        verifyNoInteractions(mail);
        verify(tokens, never()).save(any(), any(), any(), any());
    }

    @Test
    void a_deactivated_account_gets_nothing() {
        UserProfile off = new UserProfile(jane.id(), "jane", "jane@test.com", false, Role.USER, now, null);
        when(profiles.findByIdentifier("jane")).thenReturn(Optional.of(off));

        handler.request("jane");

        verifyNoInteractions(mail);
    }

    @Test
    void the_account_is_locked_before_its_last_link_is_looked_at() {
        when(profiles.findByIdentifier("jane")).thenReturn(Optional.of(jane));

        handler.request("jane");

        // Otherwise two requests at the same moment both find no recent link and both send one.
        InOrder order = inOrder(accountLock, tokens);
        order.verify(accountLock).lockFor(jane.id());
        order.verify(tokens).latestIssuedAt(jane.id());
    }

    @Test
    void a_second_request_within_five_minutes_sends_nothing_and_keeps_the_first_link() {
        when(profiles.findByIdentifier("jane")).thenReturn(Optional.of(jane));
        when(tokens.latestIssuedAt(jane.id())).thenReturn(Optional.of(now.minus(Duration.ofMinutes(4))));

        handler.request("jane");

        verifyNoInteractions(mail);
        verify(tokens, never()).deleteAllForUser(any());
    }

    @Test
    void a_blank_identifier_sends_nothing() {
        handler.request("   ");

        verifyNoInteractions(profiles, mail);
    }
}
