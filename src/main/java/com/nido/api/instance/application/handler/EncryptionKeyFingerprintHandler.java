package com.nido.api.instance.application.handler;

import com.nido.api.instance.application.port.in.ConfirmEncryptionKeyFingerprintUseCase;
import com.nido.api.instance.application.port.in.ForgetEncryptionKeyFingerprintUseCase;
import com.nido.api.instance.domain.model.KeyFingerprint;
import com.nido.api.instance.domain.port.out.InstanceStatePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/** The data's verdict on a fingerprint taken on its word — see ResolvedEncryptionKey. */
@ApplicationService
public class EncryptionKeyFingerprintHandler implements ConfirmEncryptionKeyFingerprintUseCase, ForgetEncryptionKeyFingerprintUseCase {

    private static final Logger log = LoggerFactory.getLogger(EncryptionKeyFingerprintHandler.class);

    private final InstanceStatePort instanceState;

    public EncryptionKeyFingerprintHandler(InstanceStatePort instanceState) {
        this.instanceState = instanceState;
    }

    @Override
    @Transactional
    public void confirm(KeyFingerprint fingerprint) {
        if (instanceState.confirmFingerprint(fingerprint)) {
            log.info("The data already encrypted opens with the encryption key: its fingerprint is kept");
        }
    }

    @Override
    @Transactional
    public void forget(KeyFingerprint fingerprint) {
        if (instanceState.forgetFingerprint(fingerprint)) {
            log.warn("The encryption key fingerprint was erased: the data already encrypted does not decrypt with that "
                + "key, and the right one must be able to start");
        } else {
            log.info("The encryption key fingerprint was not erased: another start replaced or confirmed it");
        }
    }
}
