package com.nido.api.infrastructure.config;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Registers the annotated bean only when outgoing mail is off (NIDO_SMTP_HOST empty or unset). */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(MailSwitch.Off.class)
public @interface ConditionalOnMailDisabled {
}
