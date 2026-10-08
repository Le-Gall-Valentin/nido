package com.nido.api.mfa.infrastructure.security;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.port.out.TotpAttemptPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class RedisTotpAttemptStoreIT {

    @Autowired TotpAttemptPort attempts;
    @Autowired StringRedisTemplate redis;

    private final UUID account = UUID.randomUUID();

    @BeforeEach
    void clean() {
        redis.delete("totp:disable:attempts:" + account);
    }

    @Test
    void tries_sent_at_once_each_get_a_turn_of_their_own() throws Exception {
        // What keeps a burst of guesses from all being read as if each were the first.
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<Integer>> tries = IntStream.range(0, 20)
                .<Callable<Integer>>mapToObj(i -> () -> attempts.record(account, CodePurpose.DISABLE)).toList();
            List<Integer> turns = pool.invokeAll(tries).stream().map(RedisTotpAttemptStoreIT::join).toList();

            assertThat(turns).containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, 20).boxed().toList());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void the_window_is_opened_by_the_first_try_and_lives_under_the_totp_prefix() {
        attempts.record(account, CodePurpose.DISABLE);

        assertThat(redis.getExpire("totp:disable:attempts:" + account, TimeUnit.SECONDS)).isBetween(1L, 15 * 60L);
    }

    private static Integer join(Future<Integer> turn) {
        try {
            return turn.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
