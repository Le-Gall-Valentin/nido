package com.nido.api.infrastructure.config;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * The one place that decides whether outgoing mail is on: {@code nido.mail.host} has text.
 *
 * <p>Spring Boot's own mail auto-configuration is deliberately not the switch. It activates as soon
 * as {@code spring.mail.host} is <i>present</i>, even empty, so {@code ${NIDO_SMTP_HOST:}} would have
 * built a sender with no host. Read through a {@link Binder} so the uppercase {@code NIDO:} keys of
 * application.yaml resolve like any other.
 */
public final class MailSwitch {

    private MailSwitch() {}

    static boolean isOn(Environment environment) {
        return Binder.get(environment).bind("nido.mail.host", String.class)
            .map(StringUtils::hasText)
            .orElse(false);
    }

    static final class On implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return isOn(context.getEnvironment());
        }
    }

    static final class Off implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !isOn(context.getEnvironment());
        }
    }
}
