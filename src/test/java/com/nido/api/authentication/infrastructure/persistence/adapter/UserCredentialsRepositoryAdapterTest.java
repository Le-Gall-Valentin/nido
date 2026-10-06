package com.nido.api.authentication.infrastructure.persistence.adapter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.authentication.domain.model.AccountInvitation;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCredentialsRepositoryAdapterTest {

    @Mock UserProfilePort userProfilePort;
    @Mock UserCredentialJpaRepository credentialRepo;
    @Mock AccountInvitationRepository invitations;

    private UserCredentialsRepositoryAdapter adapter;

    private final UUID userId = UUID.randomUUID();
    private final UserProfile userProfile =
            new UserProfile(userId, "alice", "alice@test.com", true, Role.USER, Instant.now(), null);

    @BeforeEach
    void setUp() {
        adapter = new UserCredentialsRepositoryAdapter(userProfilePort, credentialRepo, invitations);
    }

    private UserCredentialEntity entityWithHash(String hash) {
        UserCredentialEntity entity = new UserCredentialEntity();
        entity.setPasswordHash(hash);
        return entity;
    }

    @Test
    void findByIdentifier_userFound_credentialFound_returnsUserCredentials() {
        when(userProfilePort.findByIdentifier("alice")).thenReturn(Optional.of(userProfile));
        when(credentialRepo.findById(userId)).thenReturn(Optional.of(entityWithHash("hashed_pw")));

        Optional<UserCredentials> result = adapter.findByIdentifier("alice");

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
    void findByIdentifier_userNotFound_returnsEmpty() {
        when(userProfilePort.findByIdentifier("unknown")).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findByIdentifier("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdentifier_credentialNotFound_returnsEmpty() {
        when(userProfilePort.findByIdentifier("alice")).thenReturn(Optional.of(userProfile));
        when(credentialRepo.findById(userId)).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findByIdentifier("alice");

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
    void an_invited_account_without_credentials_is_not_a_data_integrity_error() {
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        Logger logger = (Logger) LoggerFactory.getLogger(UserCredentialsRepositoryAdapter.class);
        logger.addAppender(logged);
        try {
            when(userProfilePort.findById(userId)).thenReturn(Optional.of(userProfile));
            when(credentialRepo.findById(userId)).thenReturn(Optional.empty());
            when(invitations.findByUserId(userId))
                .thenReturn(Optional.of(new AccountInvitation(userId, Instant.now(), Instant.now().plusSeconds(60))));

            assertThat(adapter.findById(userId)).isEmpty();
            assertThat(logged.list).noneMatch(event -> event.getLevel() == Level.ERROR);
        } finally {
            logger.detachAppender(logged);
        }
    }

    @Test
    void findById_userNotFound_returnsEmpty() {
        when(userProfilePort.findById(userId)).thenReturn(Optional.empty());

        Optional<UserCredentials> result = adapter.findById(userId);

        assertThat(result).isEmpty();
    }

}