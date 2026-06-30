package com.synapsys.api.authentication.application.handler;

import com.synapsys.api.TestHashUtils;
import com.synapsys.api.authentication.domain.model.RefreshToken;
import com.synapsys.api.authentication.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutHandlerTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock RefreshTokenRevocationPort revocationPort;
    @Mock TokenHashPort tokenHashPort;

    private LogoutHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LogoutHandler(refreshTokenRepository, revocationPort, tokenHashPort);
        lenient().when(tokenHashPort.hash(any(String.class)))
            .thenAnswer(i -> TestHashUtils.sha256(i.getArgument(0, String.class)));
    }

    @Test
    void logout_validToken_revokesIt() {
        String raw = "valid-token";
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(), UUID.randomUUID(), TestHashUtils.sha256(raw),
            Instant.now().plusSeconds(3600), false, Instant.now(), null
        );
        when(refreshTokenRepository.findByTokenHash(TestHashUtils.sha256(raw)))
            .thenReturn(Optional.of(stored));

        handler.logout(raw);

        verify(revocationPort).revoke(stored.id());
    }

    @Test
    void logout_unknownToken_isIdempotent() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatNoException().isThrownBy(() -> handler.logout("unknown"));
        verify(revocationPort, never()).revoke(any());
    }

    @Test
    void logout_nullToken_isIdempotent() {
        assertThatNoException().isThrownBy(() -> handler.logout(null));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void logout_blankToken_isIdempotent() {
        assertThatNoException().isThrownBy(() -> handler.logout("   "));
        verifyNoInteractions(refreshTokenRepository);
    }
}