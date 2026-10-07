package com.nido.api.instance.infrastructure.config;

import com.nido.api.infrastructure.sealing.StartKey;
import com.nido.api.instance.application.port.in.ForgetEncryptionKeyFingerprintUseCase;
import com.nido.api.instance.domain.model.KeyFingerprint;
import com.nido.api.instance.domain.model.ResolvedEncryptionKey;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EncryptionKeyConfigurationTest {

    private final ForgetEncryptionKeyFingerprintUseCase forget = mock(ForgetEncryptionKeyFingerprintUseCase.class);

    @Test
    void the_key_check_learns_a_fingerprint_recorded_at_this_start_and_can_take_it_back() {
        KeyFingerprint recorded = KeyFingerprint.of("the-key-of-this-start");
        StartKey key = new EncryptionKeyConfiguration()
            .startKey(new ResolvedEncryptionKey("the-key-of-this-start", Optional.of(recorded)), forget);

        assertThat(key.fingerprintRecordedAtThisStart()).isTrue();
        key.forgetFingerprintRecordedAtThisStart();
        verify(forget).forget(recorded);
    }

    @Test
    void a_key_the_installation_already_knew_has_nothing_to_take_back() {
        StartKey key = new EncryptionKeyConfiguration()
            .startKey(new ResolvedEncryptionKey("the-key-of-this-start", Optional.empty()), forget);

        assertThat(key.fingerprintRecordedAtThisStart()).isFalse();
        key.forgetFingerprintRecordedAtThisStart();
        verifyNoInteractions(forget);
    }
}
