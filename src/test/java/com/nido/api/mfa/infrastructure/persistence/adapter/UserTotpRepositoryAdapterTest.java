package com.nido.api.mfa.infrastructure.persistence.adapter;

import com.nido.api.mfa.domain.model.UserTotpProfile;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import com.nido.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTotpRepositoryAdapterTest {

    @Mock UserTotpJpaRepository jpa;
    @Mock TotpEncryptorFactory encryptorFactory;
    @Mock TextEncryptor encryptor;

    private UserTotpRepositoryAdapter adapter;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new UserTotpRepositoryAdapter(jpa, encryptorFactory);
    }

    @Test
    void findById_withEncryptedSecret_decryptsAndReturnsProfile() {
        UserTotpEntity entity = new UserTotpEntity();
        entity.setUserId(userId);
        entity.setTotpSecret("encrypted-secret");
        entity.setTotpEnabled(true);
        when(encryptorFactory.forUser(userId)).thenReturn(encryptor);
        when(jpa.findById(userId)).thenReturn(Optional.of(entity));
        when(encryptor.decrypt("encrypted-secret")).thenReturn("plain-secret");

        Optional<UserTotpProfile> result = adapter.findById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().totpSecret()).contains("plain-secret");
        assertThat(result.get().totpEnabled()).isTrue();
    }

    @Test
    void findById_noSecret_returnsProfileWithEmptyOptional() {
        UserTotpEntity entity = new UserTotpEntity();
        entity.setUserId(userId);
        entity.setTotpEnabled(false);
        when(jpa.findById(userId)).thenReturn(Optional.of(entity));

        Optional<UserTotpProfile> result = adapter.findById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().totpSecret()).isEmpty();
    }

    @Test
    void findById_entityNotFound_returnsEmpty() {
        when(jpa.findById(userId)).thenReturn(Optional.empty());

        assertThat(adapter.findById(userId)).isEmpty();
    }

    @Test
    void createDefaultRecord_savesEntityWithUserId() {
        adapter.createDefaultRecord(userId);

        verify(jpa).save(argThat(e -> userId.equals(e.getUserId())));
    }

    @Test
    void createDefaultRecord_concurrentDuplicate_completesNormally() {
        when(jpa.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatCode(() -> adapter.createDefaultRecord(userId))
            .doesNotThrowAnyException();
    }

    @Test
    void enableTotp_writesTheProvenSecretAndFlipsTheFlagTogether() {
        // One statement, not two: until the enrolment is proven the secret only existed for the
        // length of it, so this write is what makes it the account's authenticator. Encrypted on
        // the way in, like every other secret this adapter stores.
        when(encryptorFactory.forUser(userId)).thenReturn(encryptor);
        when(encryptor.encrypt("proven-secret")).thenReturn("encrypted-secret");

        adapter.enableTotp(userId, "proven-secret");

        verify(jpa).enableTotpById(userId, "encrypted-secret");
    }

    @Test
    void disableTotp_delegatesToJpa() {
        adapter.disableTotp(userId);

        verify(jpa).disableTotpById(userId);
    }
}