package com.synapsys.api.authentication.infrastructure.persistence.adapter;

import com.synapsys.api.authentication.domain.model.RefreshToken;
import com.synapsys.api.authentication.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryAdapterTest {

    @Mock RefreshTokenJpaRepository jpa;
    @InjectMocks RefreshTokenRepositoryAdapter adapter;

    @Test
    void findByTokenHash_mapsEntityToDomain() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        RefreshTokenEntity entity = new RefreshTokenEntity(userId, "hash123", expiresAt);
        when(jpa.findByTokenHash("hash123")).thenReturn(Optional.of(entity));

        Optional<RefreshToken> result = adapter.findByTokenHash("hash123");

        assertThat(result).isPresent();
        assertThat(result.get().tokenHash()).isEqualTo("hash123");
        assertThat(result.get().userId()).isEqualTo(userId);
        assertThat(result.get().expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void findByTokenHash_notFound_returnsEmpty() {
        when(jpa.findByTokenHash("unknown")).thenReturn(Optional.empty());

        assertThat(adapter.findByTokenHash("unknown")).isEmpty();
    }

    @Test
    void tryMarkUsedAndRevoke_rowUpdated_returnsTrue() {
        UUID tokenId = UUID.randomUUID();
        when(jpa.markUsedAndRevokeIfNotRevokedById(eq(tokenId), any(Instant.class))).thenReturn(1);
        assertThat(adapter.tryMarkUsedAndRevoke(tokenId)).isTrue();
    }

    @Test
    void tryMarkUsedAndRevoke_alreadyRevoked_returnsFalse() {
        UUID tokenId = UUID.randomUUID();
        when(jpa.markUsedAndRevokeIfNotRevokedById(eq(tokenId), any(Instant.class))).thenReturn(0);
        assertThat(adapter.tryMarkUsedAndRevoke(tokenId)).isFalse();
    }

    @Test
    void revoke_delegatesToJpa() {
        UUID tokenId = UUID.randomUUID();
        adapter.revoke(tokenId);
        verify(jpa).revokeById(tokenId);
    }

    @Test
    void revokeAllForUser_delegatesToJpa() {
        UUID userId = UUID.randomUUID();
        adapter.revokeAllForUser(userId);
        verify(jpa).revokeAllByUserId(userId);
    }
}