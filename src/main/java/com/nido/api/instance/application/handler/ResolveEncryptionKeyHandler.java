package com.nido.api.instance.application.handler;

import com.nido.api.instance.application.port.in.ConfirmEncryptionKeyUseCase;
import com.nido.api.instance.application.port.in.ResolveEncryptionKeyUseCase;
import com.nido.api.instance.domain.model.EncryptionKeyDecision;
import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.KeyFingerprint;
import com.nido.api.instance.domain.model.ProvidedKey;
import com.nido.api.instance.domain.port.out.InstanceStatePort;
import com.nido.api.instance.domain.port.out.KeyFilePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@ApplicationService
public class ResolveEncryptionKeyHandler implements ResolveEncryptionKeyUseCase, ConfirmEncryptionKeyUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResolveEncryptionKeyHandler.class);

    private final InstanceStatePort instanceState;
    private final KeyFilePort keyFile;

    /** The fingerprint this start recorded for a key it was given, until the data confirms it. */
    private KeyFingerprint recordedAtThisStart;

    public ResolveEncryptionKeyHandler(InstanceStatePort instanceState, KeyFilePort keyFile) {
        this.instanceState = instanceState;
        this.keyFile = keyFile;
    }

    @Override
    @Transactional
    public String resolve(String configuredKey) {
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
            case EncryptionKeyDecision.Use use -> use.key();
            case EncryptionKeyDecision.RecordFingerprint record -> {
                recordedAtThisStart = KeyFingerprint.of(record.key());
                instanceState.recordFingerprint(recordedAtThisStart, fromDataDirectory);
                log.info("Encryption key fingerprint recorded: from now on, a start with another key is refused");
                yield record.key();
            }
            case EncryptionKeyDecision.Generate generate -> {
                String key = keyFile.create();
                instanceState.recordFingerprint(KeyFingerprint.of(key), true);
                log.warn("No encryption key was provided: one was generated in {}. Back it up, away from your "
                    + "database dumps — without it, the encrypted data cannot be read.", keyFile.location());
                yield key;
            }
            case EncryptionKeyDecision.Refuse refuse -> throw new IllegalStateException(refuse.reason());
        };
    }

    @Override
    public boolean recordedAtThisStart() {
        return recordedAtThisStart != null;
    }

    @Override
    @Transactional
    public void forgetFingerprintRecordedAtThisStart() {
        if (recordedAtThisStart != null) {
            instanceState.forgetFingerprint(recordedAtThisStart);
            recordedAtThisStart = null;
            log.warn("The encryption key fingerprint recorded at this start was erased: the data already encrypted "
                + "does not decrypt with that key");
        }
    }
}
