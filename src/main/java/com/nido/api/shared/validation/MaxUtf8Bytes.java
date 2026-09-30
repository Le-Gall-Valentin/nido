package com.nido.api.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * At most {@link #value()} bytes once encoded in UTF-8 — for what is measured in bytes rather than in
 * characters, such as what bcrypt reads of a password. An accented letter takes two bytes, an emoji
 * four, so a string can be well within a character count and past its byte count. Null is valid.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {
    int value();
    String message() default "must not be longer than {value} bytes";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
