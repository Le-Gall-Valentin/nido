package com.synapsys.api.authentication.infrastructure.persistence.repository;

import com.synapsys.api.shared.model.Role;
import com.synapsys.api.authentication.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.synapsys.api.IntegrationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class RefreshTokenRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired UserIdentityJpaRepository userIdentityJpaRepository;
    @Autowired UserCredentialJpaRepository userCredentialJpaRepository;
    @Autowired UserTotpJpaRepository userTotpJpaRepository;

    private UserIdentityEntity savedUser;

    @BeforeEach
    void setUp() {
        refreshTokenJpaRepository.deleteAll();
        userTotpJpaRepository.deleteAll();
        userCredentialJpaRepository.deleteAll();
        userIdentityJpaRepository.deleteAll();

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("repo-test-user");
        user.setEmail("repo@test.com");
        user.setRole(Role.USER);
        savedUser = userIdentityJpaRepository.save(user);
    }

    @Test
    void deleteExpiredAndOldRevoked_deletesRevokedToken_whenLastUsedAtNullAndCreatedAtBeforeCutoff() {
        Instant futureExpiry = Instant.now().plusSeconds(3600);
        RefreshTokenEntity token = new RefreshTokenEntity(savedUser.getId(), "hash-null-used", futureExpiry);
        token.setRevoked(true);
        refreshTokenJpaRepository.save(token);

        Instant now = Instant.now();
        Instant cutoffAfterCreation = now.plusSeconds(60);

        int deleted = refreshTokenJpaRepository.deleteExpiredAndOldRevoked(now, cutoffAfterCreation);

        assertThat(deleted).isEqualTo(1);
        assertThat(refreshTokenJpaRepository.findByTokenHash("hash-null-used")).isEmpty();
    }

    @Test
    void deleteExpiredAndOldRevoked_keepsRevokedToken_whenLastUsedAtNullButCreatedAtAfterCutoff() {
        Instant futureExpiry = Instant.now().plusSeconds(3600);
        RefreshTokenEntity token = new RefreshTokenEntity(savedUser.getId(), "hash-fresh", futureExpiry);
        token.setRevoked(true);
        refreshTokenJpaRepository.save(token);

        Instant now = Instant.now();
        Instant cutoffBeforeCreation = now.minusSeconds(3600);

        int deleted = refreshTokenJpaRepository.deleteExpiredAndOldRevoked(now, cutoffBeforeCreation);

        assertThat(deleted).isEqualTo(0);
        assertThat(refreshTokenJpaRepository.findByTokenHash("hash-fresh")).isPresent();
    }
}