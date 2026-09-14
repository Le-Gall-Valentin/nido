package com.nido.api.infrastructure.ratelimit;

import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rate limiter after the Redis connection factory has been stopped and started again.
 *
 * <p>Every other Redis store in this application goes through {@code StringRedisTemplate}, which
 * borrows a connection per call and therefore survives this on its own. The rate limiter is the
 * exception: bucket4j is handed one Lettuce connection, and that connection was derived once from
 * the factory's native client. {@link LettuceConnectionFactory#stop()} disposes that client and
 * {@code start()} creates a different one, so a captured connection is dead for the rest of the
 * process — every limited route then answers 500 with {@code RedisException: Connection is closed}.
 *
 * <p>Why this is worth a test rather than a note: Spring Framework 7 pauses a cached test context
 * when another one is used and restarts it on the way back ({@code spring.test.context.cache.pause}
 * defaults to {@code ON_CONTEXT_SWITCH}), which calls exactly these two methods. Sharing one
 * container across the suite made contexts shared too, and the whole suite failed the first time it
 * ran in a different order. The same call pair is what an actuator restart or a refresh scope does.
 */
@IntegrationTestConfig
class RateLimitAcrossRedisRestartIT {

    @Autowired RateLimitBucketStore store;
    @Autowired LettuceConnectionFactory connectionFactory;

    @Test
    void a_bucket_is_still_reachable_after_the_connection_factory_is_restarted() {
        String key = "restart:" + UUID.randomUUID();
        assertThat(store.tryConsume(key, 5, 60).allowed()).isTrue();

        connectionFactory.stop();
        connectionFactory.start();

        assertThat(store.tryConsume(key, 5, 60).allowed())
            .as("the limiter must not be the one thing a restart leaves broken")
            .isTrue();
    }

    @Test
    void the_bucket_kept_the_tokens_it_had_already_spent() {
        // A reconnection that silently started a new bucket would turn the restart into a free pass:
        // whoever was one request from the limit gets the whole allowance back.
        String key = "restart:" + UUID.randomUUID();
        store.tryConsume(key, 2, 60);

        connectionFactory.stop();
        connectionFactory.start();

        assertThat(store.tryConsume(key, 2, 60).remaining()).isZero();
    }
}
