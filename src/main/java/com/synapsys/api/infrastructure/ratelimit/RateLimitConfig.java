package com.synapsys.api.infrastructure.ratelimit;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}