package com.nido.api.infrastructure.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Advisor rateLimitAdvisor(RateLimitMethodInterceptor interceptor) {
        // Match methods annotated with @RateLimiting (single) OR @RateLimitingList (multiple)
        Pointcut single   = AnnotationMatchingPointcut.forMethodAnnotation(RateLimiting.class);
        Pointcut multiple = AnnotationMatchingPointcut.forMethodAnnotation(RateLimitingList.class);
        Pointcut combined = new ComposablePointcut(single).union((Pointcut) multiple);
        return new DefaultPointcutAdvisor(combined, interceptor);
    }

    @Bean
    RebindingLettuceCommands rateLimitCommands(LettuceConnectionFactory connectionFactory) {
        return new RebindingLettuceCommands(connectionFactory);
    }

    /**
     * The buckets live in Redis so that every instance counts the same request once, and they are
     * reached through {@link RebindingLettuceCommands} rather than a connection captured here — see
     * that class for what a captured one costs.
     *
     * <p>The expiry strategy is bucket4j's own: a bucket key is dropped once enough time has passed
     * for it to have refilled to capacity, so an idle caller leaves nothing behind while a busy one
     * keeps its state for as long as it still means something.
     */
    @Bean
    ProxyManager<String> rateLimitProxyManager(RebindingLettuceCommands rateLimitCommands) {
        return Bucket4jLettuce.casBasedBuilder(rateLimitCommands.asCommands())
            .expirationAfterWrite(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(1)))
            .build();
    }
}