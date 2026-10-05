package com.nido.api.authentication.application.service;

import com.nido.api.authentication.application.dto.InvitationDelivery;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.authentication.domain.port.out.PublicUrlPort;
import com.nido.api.authentication.domain.port.out.ResetTokenGeneratorPort;
import com.nido.api.authentication.domain.port.out.TokenHashPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitationIssuerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final Instant IN_A_WEEK = Instant.parse("2026-10-12T10:00:00Z");

    @Mock AccountInvitationRepository invitations;
    @Mock ResetTokenGeneratorPort generator;
    @Mock TokenHashPort hasher;
    @Mock AccountMailPort mail;
    @Mock PublicUrlPort publicUrl;

    private final AccountContact carol = new AccountContact(UUID.randomUUID(), "carol", "carol@test.com", null);
    private InvitationIssuer issuer;

    @BeforeEach
    void setUp() {
        issuer = new InvitationIssuer(invitations, generator, hasher, mail, publicUrl, Clock.fixed(NOW, ZoneOffset.UTC));
        when(generator.newToken()).thenReturn("RAW");
        when(hasher.hash("RAW")).thenReturn("HASH");
    }

    @Test
    void with_mail_on_the_link_is_mailed_and_never_handed_back() {
        when(mail.canSend()).thenReturn(true);

        InvitationDelivery delivery = issuer.issue(carol, "bob");

        assertThat(delivery).isEqualTo(new InvitationDelivery.Mailed());
        verify(invitations).save(carol.userId(), "HASH", NOW, IN_A_WEEK);
        verify(mail).accountInvitation(carol, "RAW", IN_A_WEEK, "bob");
    }

    @Test
    void with_mail_off_the_link_is_handed_back_on_the_public_address() {
        when(mail.canSend()).thenReturn(false);
        when(publicUrl.publicUrl()).thenReturn(Optional.of("https://nido.example"));

        InvitationDelivery delivery = issuer.issue(carol, "bob");

        assertThat(delivery).isEqualTo(new InvitationDelivery.Link("https://nido.example/welcome#token=RAW"));
        verify(invitations).save(carol.userId(), "HASH", NOW, IN_A_WEEK);
        verify(mail, never()).accountInvitation(any(), any(), any(), any());
    }

    @Test
    void without_a_public_address_only_the_path_is_handed_back() {
        when(mail.canSend()).thenReturn(false);
        when(publicUrl.publicUrl()).thenReturn(Optional.empty());

        assertThat(issuer.issue(carol, "bob")).isEqualTo(new InvitationDelivery.Link("/welcome#token=RAW"));
    }

    @Test
    void a_link_never_shows_its_token_in_a_log_line() {
        assertThat(new InvitationDelivery.Link("https://nido.example/welcome#token=RAW").toString())
            .doesNotContain("RAW");
    }
}
