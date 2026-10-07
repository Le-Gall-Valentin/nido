package com.nido.api.instance.application.handler;

import com.nido.api.instance.application.port.in.ForgetEncryptionKeyFingerprintUseCase;
import com.nido.api.instance.application.port.in.ResolveEncryptionKeyUseCase;
import com.nido.api.instance.domain.model.EncryptionKeyDecision;
import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.KeyFingerprint;
import com.nido.api.instance.domain.model.ProvidedKey;
import com.nido.api.instance.domain.model.ResolvedEncryptionKey;
import com.nido.api.instance.domain.port.out.InstanceStatePort;
import com.nido.api.instance.domain.port.out.KeyFilePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@ApplicationService
public class ResolveEncryptionKeyHandler implements ResolveEncryptionKeyUseCase, ForgetEncryptionKeyFingerprintUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResolveEncryptionKeyHandler.class);

    private final InstanceStatePort instanceState;
    private final KeyFilePort keyFile;

    public ResolveEncryptionKeyHandler(InstanceStatePort instanceState, KeyFilePort keyFile) {
        this.instanceState = instanceState;
        this.keyFile = keyFile;
    }

    @Override
    @Transactional
    public ResolvedEncryptionKey resolve(String configuredKey) {
        Optional<ProvidedKey> configured = Optional.ofNullable(configuredKey)
            .filter(key -> !key.isBlank())
            .map(key -> new ProvidedKey(key, "NIDO_ENCRYPTION_SECRET"));
        Optional<ProvidedKey> provided = configured
            .or(() -> keyFile.read().map(key -> new ProvidedKey(key, keyFile.location())));
        // A key of the data directory is the one Nido generated — or one put there in its place: the setup
        // shows it, even when the start that wrote it stopped before recording its fingerprint.
        boolean fromDataDirectory = configured.isEmpty() && provided.isPresent();
        InstanceState state = instanceState.load();
        return switch (EncryptionKeyDecision.decide(provided, state, keyFile.location())) {
            case EncryptionKeyDecision.Use use -> new ResolvedEncryptionKey(use.key(), Optional.empty());
            case EncryptionKeyDecision.RecordFingerprint record -> {
                // Taken on the data's word: the key check gives this back to forget() if the data refuses the key.
                KeyFingerprint recorded = KeyFingerprint.of(record.key());
                instanceState.recordFingerprint(recorded, fromDataDirectory);
                log.info("Encryption key fingerprint recorded: from now on, a start with another key is refused");
                yield new ResolvedEncryptionKey(record.key(), Optional.of(recorded));
            }
            case EncryptionKeyDecision.Generate generate -> {
                String key = keyFile.create();
                instanceState.recordFingerprint(KeyFingerprint.of(key), true);
                log.warn("No encryption key was provided: one was generated in {}. Back it up, away from your "
                    + "database dumps — without it, the encrypted data cannot be read.", keyFile.location());
                yield new ResolvedEncryptionKey(key, Optional.empty());
            }
            case EncryptionKeyDecision.Refuse refuse -> throw new IllegalStateException(refuse.reason());
        };
    }

    @Override
    @Transactional
    public void forget(KeyFingerprint fingerprint) {
        instanceState.forgetFingerprint(fingerprint);
        log.warn("The encryption key fingerprint recorded at this start was erased: the data already encrypted "
            + "does not decrypt with that key");
    }
}
