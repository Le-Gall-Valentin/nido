package com.nido.api.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What Nido accepts as a new password: 8 characters or more, 72 bytes at most, with an uppercase
 * letter, a digit and a character that is neither. One definition for every form that sets a
 * password — account creation, the account page, the reset page — so they can never disagree. The
 * frontend mirrors it in {@code shared/lib/passwordPolicy.ts}.
 *
 * <p>The upper bound is in bytes because bcrypt reads 72 bytes and refuses to hash more: counted in
 * characters, 72 accented letters (141 bytes) passed validation and failed the hashing with a 500.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank
@Size(min = 8)
@MaxUtf8Bytes(72)
@Pattern(
    regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
    message = "must contain at least one uppercase letter, one digit, and one special character"
)
public @interface StrongPassword {
    String message() default "is not a strong enough password";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
