package com.nido.api.space.infrastructure.config;

import com.nido.api.TestSpaces;
import com.nido.api.infrastructure.config.SpaceKeyCache;
import com.nido.api.infrastructure.encryption.DataKeys;
import com.nido.api.infrastructure.encryption.LegacyKeys;
import com.nido.api.infrastructure.sealing.SealedColumn;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.shared.security.EncryptionKey;
import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import org.junit.jupiter.api.Test;

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
    void a_space_key_is_the_current_key_of_its_salt() {
        UUID spaceId = UUID.randomUUID();
        UUID row = UUID.randomUUID();
        String sealed = sealers.forSpace(spaceId).seal(LABEL, row, "Loyer octobre");

        assertThat(SpaceSealer.of(DataKeys.current(KEY, saltFor(spaceId))).open(LABEL, row, sealed)).isEqualTo("Loyer octobre");
    }

    @Test
    void the_legacy_opener_of_a_space_reads_what_0_15_sealed_for_it() {
        UUID spaceId = UUID.randomUUID();
        UUID row = UUID.randomUUID();
        String sealed = LegacyKeys.sealedV2(KEY.value(), saltFor(spaceId), LABEL, row, "Loyer octobre");

        assertThat(new SpaceEncryptionConfig().legacySpaceOpeners(KEY, salts()).forSpace(spaceId).open(LABEL, row, sealed))
            .isEqualTo("Loyer octobre");
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
