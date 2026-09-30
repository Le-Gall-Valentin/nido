package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.service.PasswordChangeConsequences;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.PasswordResetToken;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.authentication.domain.port.out.IssuedTokenCutoffPort;
import com.nido.api.authentication.domain.port.out.PasswordHasherPort;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.nido.api.authentication.domain.port.out.TokenHashPort;
import com.nido.api.authentication.domain.port.out.UserCredentialPort;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.model.Language;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetHandlerTest {

    @Mock PasswordResetTokenRepository tokens;
    @Mock TokenHashPort hasher;
    @Mock UserProfilePort profiles;
    @Mock PasswordHasherPort passwordHasher;
    @Mock UserCredentialPort credentials;
    @Mock RefreshTokenRevocationPort refreshTokens;
    @Mock IssuedTokenCutoffPort cutoff;
    @Mock AccountMailPort mail;

    private final Instant now = Instant.parse("2026-09-28T10:00:00Z");
    private final UserProfile jane = new UserProfile(UUID.randomUUID(), "jane", "jane@test.com", true, Role.USER, now, Language.FR);
    private final PasswordResetToken live = new PasswordResetToken(UUID.randomUUID(), jane.id(), now, now.plusSeconds(1800));
    private PasswordResetHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PasswordResetHandler(tokens, hasher, profiles, passwordHasher, credentials,
            new PasswordChangeConsequences(tokens, refreshTokens, cutoff, mail), Clock.fixed(now, ZoneOffset.UTC));
        when(hasher.hash("RAW")).thenReturn("HASH");
        when(profiles.findById(jane.id())).thenReturn(Optional.of(jane));
        when(passwordHasher.hash("NewPassw0rd!")).thenReturn("$new");
    }

    @Test
    void a_live_link_of_an_active_account_checks_out() {
        when(tokens.findByHash("HASH")).thenReturn(Optional.of(live));

        assertThatCode(() -> handler.check("RAW")).doesNotThrowAnyException();
    }

    @Test
    void an_unknown_expired_or_orphaned_link_is_the_same_invalid_link() {
        when(tokens.findByHash("HASH")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.check("RAW")).isInstanceOf(AuthenticationException.InvalidResetToken.class);

        when(tokens.findByHash("HASH")).thenReturn(Optional.of(new PasswordResetToken(live.id(), jane.id(), now.minusSeconds(1800), now)));
        assertThatThrownBy(() -> handler.check("RAW")).isInstanceOf(AuthenticationException.InvalidResetToken.class);

        when(tokens.findByHash("HASH")).thenReturn(Optional.of(live));
        when(profiles.findById(jane.id())).thenReturn(Optional.of(
            new UserProfile(jane.id(), "jane", "jane@test.com", false, Role.USER, now, null)));
        assertThatThrownBy(() -> handler.check("RAW")).isInstanceOf(AuthenticationException.InvalidResetToken.class);

        assertThatThrownBy(() -> handler.check(" ")).isInstanceOf(AuthenticationException.InvalidResetToken.class);
    }

    @Test
    void confirming_sets_the_password_ends_every_session_burns_every_link_and_says_so() {
        when(tokens.consumeByHash("HASH")).thenReturn(Optional.of(live));

        handler.confirm("RAW", "NewPassw0rd!");

        verify(credentials).updatePasswordHash(jane.id(), "$new");
        verify(tokens).deleteAllForUser(jane.id());
        verify(refreshTokens).revokeAllForUser(jane.id());
        verify(cutoff).cutOffNow(jane.id());
        verify(mail).passwordChanged(AccountContact.of(jane));
    }

    @Test
    void a_used_or_expired_link_changes_nothing() {
        when(tokens.consumeByHash("HASH")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.confirm("RAW", "NewPassw0rd!"))
            .isInstanceOf(AuthenticationException.InvalidResetToken.class);

        when(tokens.consumeByHash("HASH")).thenReturn(Optional.of(
            new PasswordResetToken(live.id(), jane.id(), now.minusSeconds(1800), now)));
        assertThatThrownBy(() -> handler.confirm("RAW", "NewPassw0rd!"))
            .isInstanceOf(AuthenticationException.InvalidResetToken.class);

        verify(credentials, never()).updatePasswordHash(any(), any());
        verifyNoInteractions(refreshTokens, cutoff, mail);
    }
}
