package com.nido.api.space.infrastructure.config;

import com.nido.api.TestSpaces;
import com.nido.api.infrastructure.config.SpaceKeyCache;
import com.nido.api.infrastructure.sealing.SealedColumn;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shared.security.EncryptionKey;
import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceEncryptionConfigTest {

    private static final EncryptionKey KEY = new EncryptionKey(TestSpaces.ENCRYPTION_KEY);
    private static final SealedColumn LABEL = SealedColumn.ofSpace("finance_transactions", "label_encrypted");

    private final AtomicInteger saltReads = new AtomicInteger();
    private final SpaceSealers sealers = new SpaceEncryptionConfig().spaceSealers(new SpaceKeyCache(KEY), salts());

    // A 32-hex-char stand-in salt, deterministic per space id — the real use case reads the space's stored random
    // salt; this double only needs distinct, valid hex per id.
    private static String saltFor(UUID spaceId) {
        String hex = spaceId.toString().replace("-", "");
        return (hex + hex).substring(0, 32);
    }

    private GetSpaceEncryptionSaltUseCase salts() {
        return spaceId -> {
            saltReads.incrementAndGet();
            return saltFor(spaceId);
        };
    }

    @Test
    void a_space_key_is_the_one_finance_and_the_calendar_always_derived() {
        // Encryptors.delux(master key, salt of the space), as up to 0.13: what they encrypted can be converted.
        UUID spaceId = UUID.randomUUID();
        UUID row = UUID.randomUUID();
        String sealed = sealers.forSpace(spaceId).seal(LABEL, row, "Loyer octobre");

        assertThat(SpaceSealer.of(Encryptors.delux(KEY.value(), saltFor(spaceId))).open(LABEL, row, sealed)).isEqualTo("Loyer octobre");
    }

    @Test
    void a_value_sealed_for_a_space_opens_back_and_the_salt_is_read_once() {
        UUID spaceId = UUID.randomUUID();
        UUID row = UUID.randomUUID();

        String sealed = sealers.forSpace(spaceId).seal(LABEL, row, "Courses Carrefour 45.30€");

        assertThat(sealers.forSpace(spaceId).open(LABEL, row, sealed)).isEqualTo("Courses Carrefour 45.30€");
        assertThat(saltReads).hasValue(1);
    }

    @Test
    void a_value_sealed_for_one_space_does_not_open_in_another() {
        UUID row = UUID.randomUUID();
        String sealed = sealers.forSpace(UUID.randomUUID()).seal(LABEL, row, "Loyer");

        assertThatThrownBy(() -> sealers.forSpace(UUID.randomUUID()).open(LABEL, row, sealed))
            .isInstanceOfSatisfying(SealedValueRejected.class, e -> assertThat(e.reason()).isEqualTo(SealedValueRejected.Reason.UNDECRYPTABLE));
    }
}
