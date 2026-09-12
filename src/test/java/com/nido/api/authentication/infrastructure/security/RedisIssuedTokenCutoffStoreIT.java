package com.nido.api.authentication.infrastructure.security;

import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The cut-off is the only state this mechanism adds to a stateless token, so what matters here is
 * not that it works but that it stays small: one key per user whose rights actually changed, and
 * only for as long as a token predating the change could still be alive. Verified against a real
 * Redis, because a TTL that is silently absent looks exactly like one that is set.
 */
@IntegrationTestConfig
class RedisIssuedTokenCutoffStoreIT {

    @Autowired RedisIssuedTokenCutoffStore store;
    @Autowired StringRedisTemplate redisTemplate;

    @Test
    void a_user_nobody_touched_has_no_cutoff() {
        // The common case by far, and the one that has to stay free: a missing key means nothing to
        // check, so every request of every ordinary user costs one failed lookup and no more.
        assertThat(store.cutoffFor(UUID.randomUUID())).isEmpty();
    }

    @Test
    void a_cutoff_is_read_back_at_about_the_moment_it_was_recorded() {
        UUID userId = UUID.randomUUID();
        Instant before = Instant.now().minusSeconds(1);

        store.cutOffNow(userId);

        assertThat(store.cutoffFor(userId))
            .hasValueSatisfying(cutoff -> assertThat(cutoff).isBetween(before, Instant.now().plusSeconds(1)));
    }

    @Test
    void a_cutoff_expires_on_its_own_so_the_keyspace_cannot_grow() {
        // This is what separates a cut-off note from a revocation list that has to be purged. The
        // TTL matches the access token's lifetime: once no token predating the cut-off can still be
        // valid, the note has nothing left to reject.
        UUID userId = UUID.randomUUID();

        store.cutOffNow(userId);

        Long ttlSeconds = redisTemplate.getExpire("auth:token-cutoff:user:" + userId, TimeUnit.SECONDS);
        assertThat(ttlSeconds)
            .as("no TTL at all would make this a list that only ever grows")
            .isNotNull()
            .isGreaterThan(0L)
            .isLessThanOrEqualTo(Duration.ofMinutes(16).toSeconds());
    }

    @Test
    void an_unreadable_value_lets_the_request_through_rather_than_locking_the_user_out() {
        // The fail-open decision, exercised rather than asserted in a comment. A cache that cannot
        // answer must not be able to deny access to the whole application.
        UUID userId = UUID.randomUUID();
        redisTemplate.opsForValue().set("auth:token-cutoff:user:" + userId, "not-a-timestamp");

        assertThat(store.cutoffFor(userId)).isEmpty();
    }

    @Test
    void recording_a_cutoff_twice_keeps_the_later_one() {
        // Two demotions in a row, or a demotion followed by a deactivation: the newest cut-off wins,
        // otherwise the second change would be weaker than the first.
        UUID userId = UUID.randomUUID();
        store.cutOffNow(userId);
        Instant first = store.cutoffFor(userId).orElseThrow();

        store.cutOffNow(userId);

        assertThat(store.cutoffFor(userId)).hasValueSatisfying(
            second -> assertThat(second).isAfterOrEqualTo(first));
    }
}
