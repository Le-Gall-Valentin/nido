package com.nido.api.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.time.Duration;
import java.util.function.Function;

/**
 * The one place that decides how long a derived encryption key may live in memory, shared by the
 * per-user cache (TOTP) and the per-space one (SpaceKeyCache).
 *
 * <p><b>Why cache at all.</b> A key is derived by PBKDF2 — see DataKeys: ~130 ms on a developer machine, more on a
 * small server, against microseconds for a decryption. Deriving per call would put that on every read of a space.
 *
 * <p><b>Why bound it.</b> The previous {@code ConcurrentHashMap} never released anything: one entry
 * per user and per space touched since boot, held for the life of the JVM. Nothing an attacker can
 * inflate beyond the bound below — a key is derived for a space that exists or is being created, and
 * one whose creation failed leaves an entry that expires like any other — so the memory alone was minor.
 * The real cost of never expiring is that <b>nothing can ever be invalidated</b>: the day a salt is
 * rotated or a space re-keyed, running instances would keep encrypting with the old key, silently,
 * until the next restart. Expiring by <i>write</i> rather than by access is what closes that: a key
 * is re-derived from the current salt at least every {@link #ENTRY_TTL}, however hot it is. Expiring
 * on access would keep a busy space's key alive forever and change nothing.
 *
 * <p>The cost of expiry is one derivation (~130 ms) per key per window, on the request that finds the
 * entry gone.
 *
 * <p>Note that a derived key deliberately never leaves the JVM heap. Putting these in Redis would
 * turn a cache into a keyring: the master key lives outside the database and the salt in Postgres
 * precisely so that neither store is enough on its own.
 */
public final class EncryptorCache {

    /**
     * Far above any realistic number of live spaces or users, so eviction by size never thrashes;
     * it is a ceiling against unbounded growth, not a tuning knob. At a few hundred bytes per
     * entry this caps the two caches in the megabytes.
     */
    static final int MAX_ENTRIES = 10_000;

    /** How stale a derived key may be with respect to the salt it came from. */
    static final Duration ENTRY_TTL = Duration.ofMinutes(30);

    private EncryptorCache() {}

    public static <K> LoadingCache<K, TextEncryptor> build(Function<K, TextEncryptor> derive) {
        return build(derive, Ticker.systemTicker());
    }

    /** Visible for testing: a fake ticker makes expiry deterministic instead of a sleep. */
    static <K> LoadingCache<K, TextEncryptor> build(Function<K, TextEncryptor> derive, Ticker ticker) {
        return bounded(ticker).build(derive::apply);
    }

    /**
     * The same bounds, for a caller that only knows how to derive a key at the moment it asks for one —
     * see {@link SpaceKeyCache}, whose salt sometimes comes from the very row being read.
     */
    public static <K> Cache<K, TextEncryptor> buildWithoutLoader() {
        return buildWithoutLoader(Ticker.systemTicker());
    }

    /** Visible for testing, like {@link #build(Function, Ticker)}. */
    static <K> Cache<K, TextEncryptor> buildWithoutLoader(Ticker ticker) {
        return bounded(ticker).build();
    }

    private static Caffeine<Object, Object> bounded(Ticker ticker) {
        return Caffeine.newBuilder()
            .maximumSize(MAX_ENTRIES)
            .expireAfterWrite(ENTRY_TTL)
            .ticker(ticker);
    }
}
