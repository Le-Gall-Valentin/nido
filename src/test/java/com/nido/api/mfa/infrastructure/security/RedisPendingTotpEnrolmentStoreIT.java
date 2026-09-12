package com.nido.api.mfa.infrastructure.security;

import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Moving the enrolment out of the database must not quietly move it out of its protection too, and
 * must not lose the atomicity the old {@code UPDATE ... WHERE totp_secret IS NULL} provided. Both
 * are checked against a real Redis rather than reasoned about from the API being called.
 */
@IntegrationTestConfig
class RedisPendingTotpEnrolmentStoreIT {

    private static final String SECRET = "JBSWY3DPEHPK3PXP";

    @Autowired RedisPendingTotpEnrolmentStore store;
    @Autowired StringRedisTemplate redisTemplate;

    private static String keyFor(UUID userId) {
        return "totp:enrolment:user:" + userId;
    }

    @Test
    void an_enrolment_is_read_back_as_it_was_started() {
        UUID userId = UUID.randomUUID();

        assertThat(store.startIfAbsent(userId, SECRET)).isTrue();

        assertThat(store.find(userId)).contains(SECRET);
    }

    @Test
    void the_secret_never_reaches_redis_in_the_clear() {
        // The regression that would matter most and show no symptom: this Redis has no password,
        // no TLS, and persists to disk, so a secret written as-is would be strictly less protected
        // than the encrypted column it came from.
        UUID userId = UUID.randomUUID();
        store.startIfAbsent(userId, SECRET);

        String stored = redisTemplate.opsForValue().get(keyFor(userId));

        assertThat(stored).isNotNull().isNotEqualTo(SECRET).doesNotContain(SECRET);
    }

    @Test
    void a_second_start_leaves_the_first_enrolment_alone() {
        // What the conditional UPDATE used to guarantee: two tabs, one QR code. The loser is told
        // so and reads back what the winner wrote.
        UUID userId = UUID.randomUUID();
        store.startIfAbsent(userId, SECRET);

        boolean written = store.startIfAbsent(userId, "OTHERSECRETVALUE");

        assertThat(written).isFalse();
        assertThat(store.find(userId)).contains(SECRET);
    }

    @Test
    void an_enrolment_carries_an_expiry_so_it_cannot_outlive_the_attempt() {
        // The whole reason for the move. Without a TTL this is the old design with extra steps: an
        // unconfirmed credential sitting there indefinitely.
        UUID userId = UUID.randomUUID();
        store.startIfAbsent(userId, SECRET);

        Long ttlSeconds = redisTemplate.getExpire(keyFor(userId), TimeUnit.SECONDS);

        assertThat(ttlSeconds).isNotNull().isGreaterThan(0L).isLessThanOrEqualTo(15 * 60L);
    }

    @Test
    void a_discarded_enrolment_is_gone_and_discarding_twice_is_not_an_error() {
        UUID userId = UUID.randomUUID();
        store.startIfAbsent(userId, SECRET);

        store.discard(userId);
        store.discard(userId);

        assertThat(store.find(userId)).isEmpty();
        assertThat(store.startIfAbsent(userId, "FRESHSECRETVALUE")).isTrue();
    }

    @Test
    void two_users_enrolling_at_once_do_not_see_each_others_secret() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        store.startIfAbsent(alice, SECRET);
        store.startIfAbsent(bob, "OTHERSECRETVALUE");

        assertThat(store.find(alice)).contains(SECRET);
        assertThat(store.find(bob)).contains("OTHERSECRETVALUE");
    }
}
