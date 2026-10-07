package com.nido.api.instance.infrastructure.config;

import com.nido.api.infrastructure.sealing.StartKey;
import com.nido.api.instance.application.port.in.ConfirmEncryptionKeyFingerprintUseCase;
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

    private final ConfirmEncryptionKeyFingerprintUseCase confirm = mock(ConfirmEncryptionKeyFingerprintUseCase.class);
    private final ForgetEncryptionKeyFingerprintUseCase forget = mock(ForgetEncryptionKeyFingerprintUseCase.class);

    private StartKey startKey(Optional<KeyFingerprint> awaiting) {
        return new EncryptionKeyConfiguration().startKey(new ResolvedEncryptionKey("the-key-of-this-start", awaiting), confirm, forget);
    }

    @Test
    void the_key_check_passes_its_verdict_on_a_fingerprint_awaiting_it() {
        KeyFingerprint awaiting = KeyFingerprint.of("the-key-of-this-start");
        StartKey key = startKey(Optional.of(awaiting));

        assertThat(key.awaitsConfirmation()).isTrue();
        key.confirm();
        key.forget();
        verify(confirm).confirm(awaiting);
        verify(forget).forget(awaiting);
    }

    @Test
    void a_key_confirmed_already_has_nothing_to_confirm_or_take_back() {
        StartKey key = startKey(Optional.empty());

        assertThat(key.awaitsConfirmation()).isFalse();
        key.confirm();
        key.forget();
        verifyNoInteractions(confirm, forget);
    }
}
