package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.InvitationDelivery;
import com.nido.api.authentication.application.service.InvitationIssuer;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteAccountHandlerTest {

    @Mock UserProfilePort profiles;
    @Mock InvitationIssuer issuer;

    private final UserProfile carol = new UserProfile(UUID.randomUUID(), "carol", "carol@test.com", true, Role.USER,
        Instant.parse("2026-10-05T10:00:00Z"), null);

    @Test
    void the_account_is_invited_under_its_own_name_and_address() {
        when(profiles.findById(carol.id())).thenReturn(Optional.of(carol));
        when(issuer.issue(AccountContact.of(carol), "bob")).thenReturn(new InvitationDelivery.Mailed());

        assertThat(new InviteAccountHandler(profiles, issuer).invite(carol.id(), "bob"))
            .isEqualTo(new InvitationDelivery.Mailed());
    }

    @Test
    void an_account_that_does_not_exist_is_not_invited() {
        when(profiles.findById(carol.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new InviteAccountHandler(profiles, issuer).invite(carol.id(), "bob"))
            .isInstanceOf(AuthenticationException.UserNotFound.class);
        verifyNoInteractions(issuer);
    }
}
