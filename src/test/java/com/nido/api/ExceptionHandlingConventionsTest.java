package com.nido.api;

import com.nido.api.infrastructure.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalExceptionHandler} handles {@code Exception}, so it matches every error in the
 * application. Spring picks the <b>first advice in order</b> that has a matching handler — not the
 * most specific one across advices — so an advice sitting at the same precedence as the last resort
 * loses the coin toss and its declared domain errors come back as 500s.
 *
 * <p>That is not hypothetical: it is what happened when the last resort was added, and
 * {@code UnexpectedFailureIT} caught a {@code SpaceNotFound} turning into an internal error. A
 * default order is the easy thing to write, which is why the rule is checked rather than trusted.
 */
class ExceptionHandlingConventionsTest {

    private static List<Class<?>> advices() {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestControllerAdvice.class));
        return scanner.findCandidateComponents("com.nido.api").stream()
            .map(BeanDefinition::getBeanClassName)
            .map(ExceptionHandlingConventionsTest::load)
            .toList();
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(className, e);
        }
    }

    private static int precedenceOf(Class<?> advice) {
        Order order = advice.getAnnotation(Order.class);
        return order == null ? Ordered.LOWEST_PRECEDENCE : order.value();
    }

    @Test
    void every_advice_takes_precedence_over_the_last_resort() {
        int lastResort = precedenceOf(GlobalExceptionHandler.class);

        assertThat(advices())
            .filteredOn(advice -> advice != GlobalExceptionHandler.class)
            .allSatisfy(advice -> assertThat(precedenceOf(advice))
                .as("%s must declare @Order below %s's, or its errors become 500s",
                    advice.getSimpleName(), GlobalExceptionHandler.class.getSimpleName())
                .isLessThan(lastResort));
    }

    @Test
    void the_last_resort_is_actually_last() {
        assertThat(precedenceOf(GlobalExceptionHandler.class)).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void the_rule_actually_looked_at_something() {
        // A broken scan would make the rule above pass over an empty list forever.
        assertThat(advices())
            .as("no @RestControllerAdvice found at all — the classpath scan is broken, not the code")
            .hasSizeGreaterThan(5)
            .contains(GlobalExceptionHandler.class);
    }
}
