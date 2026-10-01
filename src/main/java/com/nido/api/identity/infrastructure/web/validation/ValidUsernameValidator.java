package com.nido.api.identity.infrastructure.web.validation;

import com.nido.api.identity.domain.model.Username;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return Username.isValid(value);
    }
}
