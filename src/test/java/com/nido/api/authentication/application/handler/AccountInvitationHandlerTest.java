package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.domain.model.AccountInvitation;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.domain.port.out.PasswordHasherPort;
import com.nido.api.authentication.domain.port.out.TokenHashPort;
import com.nido.api.authentication.domain.port.out.UserCredentialPort;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountInvitationHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    @Mock AccountInvitationRepository invitations;
    @Mock TokenHashPort hasher;
    @Mock UserProfilePort profiles;
    @Mock PasswordHasherPort passwordHasher;
    @Mock UserCredentialPort credentials;

    private final UserProfile carol = new UserProfile(UUID.randomUUID(), "carol", "carol@test.com", true, Role.USER, NOW, null);
    private final AccountInvitation live = new AccountInvitation(carol.id(), NOW.minusSeconds(60), NOW.plusSeconds(3600));
    private AccountInvitationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AccountInvitationHandler(invitations, hasher, profiles, passwordHasher, credentials,
            Clock.fixed(NOW, ZoneOffset.UTC));
        when(hasher.hash("RAW")).thenReturn("HASH");
        when(profiles.findById(carol.id())).thenReturn(Optional.of(carol));
        when(passwordHasher.hash("Welcome-Home-1")).thenReturn("BCRYPT");
    }

    @Test
    void a_live_link_names_its_account() {
        when(invitations.findByHash("HASH")).thenReturn(Optional.of(live));

        assertThat(handler.check("RAW")).isEqualTo("carol");
    }

    @Test
    void accepting_sets_the_first_password_and_nothing_else() {
        when(invitations.consumeByHash("HASH")).thenReturn(Optional.of(live));

        handler.accept("RAW", "Welcome-Home-1");

        verify(credentials).saveCredential(carol.id(), "BCRYPT");
    }

    @Test
    void an_expired_link_is_refused() {
        AccountInvitation expired = new AccountInvitation(carol.id(), NOW.minusSeconds(7200), NOW);
        when(invitations.findByHash("HASH")).thenReturn(Optional.of(expired));
        when(invitations.consumeByHash("HASH")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> handler.check("RAW")).isInstanceOf(AuthenticationException.InvalidInvitationToken.class);
        assertThatThrownBy(() -> handler.accept("RAW", "Welcome-Home-1"))
            .isInstanceOf(AuthenticationException.InvalidInvitationToken.class);
        verify(credentials, never()).saveCredential(any(), any());
    }

    @Test
    void an_unknown_or_blank_link_is_refused() {
        when(invitations.findByHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.check("RAW")).isInstanceOf(AuthenticationException.InvalidInvitationToken.class);
        assertThatThrownBy(() -> handler.check(" ")).isInstanceOf(AuthenticationException.InvalidInvitationToken.class);
        assertThatThrownBy(() -> handler.check(null)).isInstanceOf(AuthenticationException.InvalidInvitationToken.class);
    }

    @Test
    void the_link_of_a_deactivated_or_deleted_account_is_refused() {
        when(invitations.findByHash("HASH")).thenReturn(Optional.of(live));
        when(profiles.findById(carol.id()))
            .thenReturn(Optional.of(new UserProfile(carol.id(), "carol", "carol@test.com", false, Role.USER, NOW, null)))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.check("RAW")).isInstanceOf(AuthenticationException.InvalidInvitationToken.class);
        assertThatThrownBy(() -> handler.check("RAW")).isInstanceOf(AuthenticationException.InvalidInvitationToken.class);
    }
}
