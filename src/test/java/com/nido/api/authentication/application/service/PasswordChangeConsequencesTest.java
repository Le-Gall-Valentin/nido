package com.nido.api.authentication.application.service;

import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.port.out.AccountMailPort;
import com.nido.api.authentication.domain.port.out.IssuedTokenCutoffPort;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordChangeConsequencesTest {

    @Mock PasswordResetTokenRepository resetTokens;
    @Mock RefreshTokenRevocationPort refreshTokens;
    @Mock IssuedTokenCutoffPort cutoff;
    @Mock AccountMailPort mail;

    private final AccountContact jane = new AccountContact(UUID.randomUUID(), "jane", "jane@test.com", "fr");

    @Test
    void every_way_in_the_old_password_opened_is_closed() {
        new PasswordChangeConsequences(resetTokens, refreshTokens, cutoff, mail).apply(jane);

        verify(resetTokens).deleteAllForUser(jane.userId());
        verify(refreshTokens).revokeAllForUser(jane.userId());
        verify(cutoff).cutOffNow(jane.userId());
    }

    @Test
    void the_holder_is_told_once_everything_is_closed() {
        new PasswordChangeConsequences(resetTokens, refreshTokens, cutoff, mail).apply(jane);

        InOrder order = inOrder(resetTokens, refreshTokens, cutoff, mail);
        order.verify(resetTokens).deleteAllForUser(jane.userId());
        order.verify(refreshTokens).revokeAllForUser(jane.userId());
        order.verify(cutoff).cutOffNow(jane.userId());
        order.verify(mail).passwordChanged(jane);
    }
}
