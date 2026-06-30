package com.synapsys.api.infrastructure.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    ApplicationRunner redisStartupCheck(StringRedisTemplate redisTemplate) {
        return args -> {
            try {
                redisTemplate.execute(RedisConnectionCommands::ping, true);
                log.info("Redis connection established");
            } catch (Exception e) {
                throw new IllegalStateException("Redis is unavailable — cannot start application", e);
            }
        };
    }

    @Bean(destroyMethod = "close")
    StatefulRedisConnection<String, byte[]> rateLimitLettuceConnection(LettuceConnectionFactory connectionFactory) {
        if (!(connectionFactory.getNativeClient() instanceof RedisClient client)) {
            throw new IllegalStateException(
                "Expected Lettuce standalone RedisClient, got: " + connectionFactory.getNativeClient());
        }
        return client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    ProxyManager<String> rateLimitProxyManager(StatefulRedisConnection<String, byte[]> rateLimitLettuceConnection) {
        return Bucket4jLettuce.casBasedBuilder(rateLimitLettuceConnection)
            .expirationAfterWrite(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(1)))
            .build();
    }
}