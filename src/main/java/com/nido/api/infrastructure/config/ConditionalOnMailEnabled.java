package com.nido.api.infrastructure.config;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Registers the annotated bean only when outgoing mail is on (NIDO_SMTP_HOST has text). */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(MailSwitch.On.class)
public @interface ConditionalOnMailEnabled {
}
