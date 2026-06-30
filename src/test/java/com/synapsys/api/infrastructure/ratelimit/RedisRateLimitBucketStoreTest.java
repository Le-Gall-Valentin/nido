package com.synapsys.api.infrastructure.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisRateLimitBucketStoreTest {

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    private static LettuceConnectionFactory springFactory;
    private static LettuceBasedProxyManager<String> proxyManager;
    private static StringRedisTemplate redisTemplate;
    private static StatefulRedisConnection<String, byte[]> lettuceConnection;

    private RedisRateLimitBucketStore store;

    @BeforeAll
    static void setUpAll() {
        springFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        springFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(springFactory);

        RedisClient lettuceClient = (RedisClient) springFactory.getNativeClient();
        lettuceConnection = lettuceClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        proxyManager = LettuceBasedProxyManager.builderFor(lettuceConnection)
            .withExpirationStrategy(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(1)))
            .build();
    }

    @AfterAll
    static void tearDownAll() {
        lettuceConnection.close();
        springFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        store = new RedisRateLimitBucketStore(proxyManager, redisTemplate);
        store.clearAll();
    }

    @Test
    void firstMaxRequestsAreAllowed() {
        for (int i = 0; i < 10; i++) {
            assertThat(store.tryConsume("AuthController.login:IP:1.2.3.4", 10, 60).allowed())
                .as("attempt %d", i + 1).isTrue();
        }
    }

    @Test
    void requestBeyondMaxIsBlocked() {
        for (int i = 0; i < 10; i++) {
            store.tryConsume("AuthController.login:IP:1.2.3.4", 10, 60);
        }
        var result = store.tryConsume("AuthController.login:IP:1.2.3.4", 10, 60);
        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isZero();
        assertThat(result.nanosToWaitForRefill()).isPositive();
    }

    @Test
    void remainingDecrementsWithEachRequest() {
        var first = store.tryConsume("AuthController.login:IP:2.2.2.2", 5, 60);
        assertThat(first.remaining()).isEqualTo(4L);

        var second = store.tryConsume("AuthController.login:IP:2.2.2.2", 5, 60);
        assertThat(second.remaining()).isEqualTo(3L);
    }

    @Test
    void tokensRefillAfterWindow() throws InterruptedException {
        String key = "AuthController.login:IP:3.3.3.3";
        for (int i = 0; i < 5; i++) {
            store.tryConsume(key, 5, 1);
        }
        assertThat(store.tryConsume(key, 5, 1).allowed()).isFalse();

        Thread.sleep(1200);

        assertThat(store.tryConsume(key, 5, 1).allowed()).isTrue();
    }

    @Test
    void differentKeysDontInterfere() {
        for (int i = 0; i < 10; i++) {
            store.tryConsume("AuthController.login:IP:4.4.4.4", 10, 60);
        }
        assertThat(store.tryConsume("AuthController.login:IP:5.5.5.5", 10, 60).allowed()).isTrue();
    }

    @Test
    void clearAllResetsAllBuckets() {
        for (int i = 0; i < 10; i++) {
            store.tryConsume("AuthController.login:IP:6.6.6.6", 10, 60);
        }
        store.clearAll();
        assertThat(store.tryConsume("AuthController.login:IP:6.6.6.6", 10, 60).allowed()).isTrue();
    }

    @Test
    void clearAll_doesNotDeleteNonRateLimitKeys() {
        redisTemplate.opsForValue().set("other:some-session-key", "some-value");
        store.tryConsume("AuthController.login:IP:7.7.7.7", 10, 60);

        store.clearAll();

        assertThat(redisTemplate.hasKey("other:some-session-key")).isTrue();
        assertThat(store.tryConsume("AuthController.login:IP:7.7.7.7", 10, 60).allowed()).isTrue();
    }

    @Test
    void concurrentRequestsRespectLimit() throws InterruptedException {
        int threads = 30;
        int max = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger allowed = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                if (store.tryConsume("AuthController.login:IP:concurrent", max, 60).allowed()) {
                    allowed.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }
        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("All threads should complete within timeout").isTrue();
        assertThat(allowed.get()).isLessThanOrEqualTo(max);
    }
}