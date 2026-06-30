package com.synapsys.api.authentication.infrastructure.persistence.adapter;

import com.synapsys.api.authentication.domain.model.UserCredentials;
import com.synapsys.api.authentication.domain.model.UserProfile;
import com.synapsys.api.authentication.domain.port.out.UserProfilePort;
import com.synapsys.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.synapsys.api.shared.model.Role;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCredentialsRepositoryAdapterTest {

    @Mock UserProfilePort userProfilePort;
    @Mock UserCredentialJpaRepository credentialRepo;

    private UserCredentialsRepositoryAdapter adapter;

    private final UUID userId = UUID.randomUUID();
    private final UserProfile userProfile =
            new UserProfile(userId, "alice", "alice@test.com", true, Role.USER, Instant.now());

    @BeforeEach
    void setUp() {
        adapter = new UserCredentialsRepositoryAdapter(userProfilePort, credentialRepo);
    }

    private UserCredentialEntity entityWithHash(String hash) {
        UserCredentialEntity entity = new UserCredentialEntity();
        entity.setPasswordHash(hash);
        return entity;
    }

    @Test
    void findByUsername_userFound_credentialFound_returnsUserCredentials() {
        when(userProfilePort.findByUsername("alice")).thenReturn(Optional.of(userProfile));
        when(credentialRepo.findById(userId)).thenReturn(Optional.of(entityWithHash("hashed_pw")));

        Optional<UserCredentials> result = adapter.findByUsername("alice");

        assertThat(result).isPresent();
        UserCredentials creds = result.get();
        assertThat(creds.id()).isEqualTo(userId);
        assertThat(creds.username()).isEqualTo("alice");
        assertThat(creds.email()).isEqualTo("alice@test.com");
        assertThat(creds.passwordHash()).isEqualTo("hashed_pw");
        assertThat(creds.isActive()).isTrue();
        assertThat(creds.role()).isEqualTo(Role.USER);
    }

    @Test
    void findByUsername_userNotFound_returnsEmpty() {
        when(userProfilePort.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findByUsername("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findByUsername_credentialNotFound_returnsEmpty() {
        when(userProfilePort.findByUsername("alice")).thenReturn(Optional.of(userProfile));
        when(credentialRepo.findById(userId)).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findByUsername("alice");

        assertThat(result).isEmpty();
    }

    @Test
    void findById_userFound_credentialFound_returnsUserCredentials() {
        when(userProfilePort.findById(userId)).thenReturn(Optional.of(userProfile));
        when(credentialRepo.findById(userId)).thenReturn(Optional.of(entityWithHash("hashed_pw")));

        Optional<UserCredentials> result = adapter.findById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(userId);
        assertThat(result.get().passwordHash()).isEqualTo("hashed_pw");
    }

    @Test
    void findById_userFound_credentialAbsent_returnsEmpty() {
        when(userProfilePort.findById(userId)).thenReturn(Optional.of(userProfile));
        when(credentialRepo.findById(userId)).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findById(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void findById_userNotFound_returnsEmpty() {
        when(userProfilePort.findById(userId)).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findById(userId);

        assertThat(result).isEmpty();
    }

}