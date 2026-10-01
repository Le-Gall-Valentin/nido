package com.nido.api.identity.infrastructure.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A request field holding a username the domain accepts ({@link com.nido.api.identity.domain.model.Username}):
 * the request is refused with a 400 before anything runs, and the rule is written once, in the domain.
 */
@Documented
@Constraint(validatedBy = ValidUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {
    String message() default "must be 3 to 50 characters, without @";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
