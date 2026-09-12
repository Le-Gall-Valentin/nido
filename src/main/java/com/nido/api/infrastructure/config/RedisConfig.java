package com.nido.api.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

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
}
