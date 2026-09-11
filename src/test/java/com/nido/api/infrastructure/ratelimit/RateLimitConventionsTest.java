package com.nido.api.infrastructure.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Counting an authenticated caller by IP charges a household, an office or a phone network as a
 * single caller: everyone behind one NAT shares a bucket and locks each other out, while an
 * attacker holding an account simply moves to another IP. The account is the unit that matters
 * once someone is through the door, which is why {@link RateLimitMode#USER_ELSE_IP} is the default.
 *
 * <p>A default protects every route written from here on without anyone having to remember it, so
 * this rule only has to catch the deliberate deviation: writing {@code mode = IP} on a route that
 * requires authentication. That is not a hypothetical mistake — it reads as the safer option, and
 * it is what this project's own audit originally recommended.
 */
class RateLimitConventionsTest {

    private static List<Class<?>> controllers() {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return scanner.findCandidateComponents("com.nido.api").stream()
            .map(BeanDefinition::getBeanClassName)
            .map(RateLimitConventionsTest::load)
            .toList();
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(className, e);
        }
    }

    /** Every rate-limit rule on a method, whether written once or stacked. */
    private static List<RateLimiting> rulesOn(Method method) {
        RateLimitingList container = method.getAnnotation(RateLimitingList.class);
        if (container != null) {
            return List.of(container.value());
        }
        RateLimiting single = method.getAnnotation(RateLimiting.class);
        return single != null ? List.of(single) : List.of();
    }

    private static boolean requiresAuthentication(Method method) {
        PreAuthorize guard = method.getAnnotation(PreAuthorize.class);
        return guard != null && (guard.value().contains("isAuthenticated") || guard.value().contains("hasRole"));
    }

    @Test
    void no_authenticated_route_is_rate_limited_by_ip_alone() {
        Set<String> offenders = new TreeSet<>();
        for (Class<?> controller : controllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!requiresAuthentication(method)) {
                    continue;
                }
                for (RateLimiting rule : rulesOn(method)) {
                    if (rule.mode() == RateLimitMode.IP) {
                        offenders.add(controller.getSimpleName() + "#" + method.getName());
                    }
                }
            }
        }

        assertThat(offenders)
            .as("an authenticated caller is identified by their account, so counting them by IP "
                + "punishes everyone sharing their connection and lets an attacker escape by "
                + "changing IP — drop the mode and take the USER_ELSE_IP default")
            .isEmpty();
    }

    @Test
    void the_default_counts_per_account_when_there_is_one() throws Exception {
        // The ninety-odd routes that declare no mode at all rest entirely on this value.
        RateLimiting declared = Defaults.class.getMethod("takesTheDefault").getAnnotation(RateLimiting.class);

        assertThat(declared.mode()).isEqualTo(RateLimitMode.USER_ELSE_IP);
    }

    @Test
    void the_rule_actually_looked_at_something() {
        // A broken scan would let the rule above pass over an empty list forever.
        long rateLimitedRoutes = controllers().stream()
            .flatMap(c -> java.util.Arrays.stream(c.getDeclaredMethods()))
            .filter(m -> !rulesOn(m).isEmpty())
            .count();

        assertThat(rateLimitedRoutes)
            .as("no rate-limited route found at all — the classpath scan is broken, not the code")
            .isGreaterThan(50);
    }

    @SuppressWarnings("unused")
    private static class Defaults {
        @RateLimiting
        public void takesTheDefault() {}
    }
}
