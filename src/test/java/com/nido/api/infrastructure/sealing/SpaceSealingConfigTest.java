package com.nido.api.infrastructure.sealing;

import com.nido.api.TestSpaces;
import com.nido.api.infrastructure.config.SpaceKeyCache;
import com.nido.api.shared.security.EncryptionKey;
import com.nido.api.space.application.port.in.GetSpaceEncryptionSaltUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceSealingConfigTest {

    private static final EncryptionKey KEY = new EncryptionKey(TestSpaces.ENCRYPTION_KEY);
    private static final SealedColumn LABEL = SealedColumn.ofSpace("finance_transactions", "label_encrypted");

    private final AtomicInteger saltReads = new AtomicInteger();
    private final SpaceSealers sealers = new SpaceSealingConfig().spaceSealers(new SpaceKeyCache(KEY), salts());

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
    void what_finance_and_the_calendar_encrypted_before_0_14_still_opens_for_the_migration() {
        UUID spaceId = UUID.randomUUID();
        // Exactly what FinanceEncryptionConfig and CalendarEncryptionConfig derived up to 0.13.
        String before = Encryptors.delux(KEY.value(), saltFor(spaceId)).encrypt("Loyer octobre");

        assertThat(sealers.forSpace(spaceId).openUnsealed(before)).isEqualTo("Loyer octobre");
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
