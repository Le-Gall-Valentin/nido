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
 * What Nido accepts as a username: 3 to 50 characters, none of them '@'. One definition for every
 * form that sets one — account creation and the account page. The '@' is refused because sign-in,
 * "forgot password" and invitations read a typed value holding one as an email address: such a
 * username could never be reached, or could stand in front of somebody's address. The database
 * refuses it too ({@code ck_users_username_no_at}); the frontend mirrors it in both forms.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank
@Size(min = 3, max = 50)
@Pattern(regexp = "[^@]*", message = "must not contain @")
public @interface Username {
    String message() default "is not a valid username";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
