package com.nido.api.infrastructure.config;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The properties the previous unbounded map did not have, plus the two it did have and that the
 * switch must not lose. All of them are invisible in normal use — a cache that never evicts and
 * never expires behaves identically to one that does, right up to the moment it matters — so they
 * are asserted here rather than assumed from the shape of the builder call.
 */
class EncryptorCacheTest {

    /** Deterministic time: expiry is asserted by moving the clock, never by sleeping. */
    private static final class FakeTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override public long read() { return nanos.get(); }

        void advance(Duration duration) { nanos.addAndGet(duration.toNanos()); }
    }

    private static TextEncryptor anyEncryptor() {
        return Encryptors.delux("0123456789abcdef0123456789abcdef", "aabbccddeeff00112233445566778899");
    }

    @Test
    void stops_growing_once_it_is_full() {
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(id -> anyEncryptor(), new FakeTicker());

        for (int i = 0; i < EncryptorCache.MAX_ENTRIES + 500; i++) {
            cache.get(UUID.randomUUID());
        }
        cache.cleanUp();

        assertThat(cache.estimatedSize()).isLessThanOrEqualTo(EncryptorCache.MAX_ENTRIES);
    }

    @Test
    void derives_a_key_once_and_reuses_it_within_the_window() {
        // The whole reason the cache exists: deriving costs ~2 ms against ~6 µs for a decryption.
        AtomicInteger derivations = new AtomicInteger();
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(
            id -> { derivations.incrementAndGet(); return anyEncryptor(); }, new FakeTicker());
        UUID key = UUID.randomUUID();

        cache.get(key);
        cache.get(key);
        cache.get(key);

        assertThat(derivations).hasValue(1);
    }

    @Test
    void re_derives_the_key_once_the_entry_has_aged_out() {
        // What the unbounded map could not do: pick up a salt that changed under it. Without this,
        // an instance would keep encrypting with the key it derived at boot until it restarts.
        AtomicInteger derivations = new AtomicInteger();
        FakeTicker ticker = new FakeTicker();
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(
            id -> { derivations.incrementAndGet(); return anyEncryptor(); }, ticker);
        UUID key = UUID.randomUUID();
        cache.get(key);

        ticker.advance(EncryptorCache.ENTRY_TTL.plusMinutes(1));
        cache.get(key);

        assertThat(derivations).hasValue(2);
    }

    @Test
    void lets_a_failed_derivation_through_untouched_and_caches_nothing() {
        // Finance derives its salt from the database, so an unknown space makes the loader throw
        // SpaceNotFound — which must still reach the caller as itself (it is what turns into a 404),
        // and must not leave an entry behind. That last part is why an unknown id cannot be used to
        // inflate the cache. Both were true of computeIfAbsent; neither is free after the switch.
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(id -> {
            throw new IllegalStateException("no such space");
        }, new FakeTicker());

        assertThatThrownBy(() -> cache.get(UUID.randomUUID()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("no such space");
        cache.cleanUp();
        assertThat(cache.estimatedSize()).isZero();
    }

    @Test
    void expires_on_write_so_a_key_in_constant_use_is_still_refreshed() {
        // expireAfterAccess would keep the busiest space's key alive forever — exactly the key
        // whose salt rotation matters most. This is what separates the two policies.
        AtomicInteger derivations = new AtomicInteger();
        FakeTicker ticker = new FakeTicker();
        LoadingCache<UUID, TextEncryptor> cache = EncryptorCache.build(
            id -> { derivations.incrementAndGet(); return anyEncryptor(); }, ticker);
        UUID key = UUID.randomUUID();

        // Read it steadily for twice the TTL, never leaving it idle long enough to expire on access.
        Duration step = EncryptorCache.ENTRY_TTL.dividedBy(4);
        for (int i = 0; i < 8; i++) {
            cache.get(key);
            ticker.advance(step);
        }

        assertThat(derivations.get())
            .as("a key read continuously must still be re-derived roughly once per TTL")
            .isGreaterThanOrEqualTo(2);
    }
}
