package com.nido.api.shared.validation;

import com.nido.api.shared.model.Language;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LanguageCodeValidator implements ConstraintValidator<LanguageCode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || Language.fromCode(value).isPresent();
    }
}
