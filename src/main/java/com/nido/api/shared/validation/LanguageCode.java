package com.nido.api.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The code of a language the app speaks — whatever {@link com.nido.api.shared.model.Language} lists,
 * so a language added there is accepted here without a second list to keep in step. Null is valid.
 */
@Documented
@Constraint(validatedBy = LanguageCodeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LanguageCode {
    String message() default "is not a language the app speaks";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
