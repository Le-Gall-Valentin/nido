package com.nido.api.instance.infrastructure.validation;

import com.nido.api.instance.domain.port.out.AccountRulesPort;
import com.nido.api.shared.validation.StrongPassword;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** The annotations of the setup request (SetupAdminRequest), applied to values that come from elsewhere. */
@Component
public class AccountRulesAdapter implements AccountRulesPort {

    /** The same constraints as the request — {@code @NotBlank} aside: "all three or none" is checked before. */
    record Account(@Email String email, @StrongPassword String password) {}

    private final Validator validator;

    public AccountRulesAdapter(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Optional<String> emailProblem(String email) {
        return worded(validator.validateValue(Account.class, "email", email));
    }

    @Override
    public Optional<String> passwordProblem(String password) {
        return worded(validator.validateValue(Account.class, "password", password));
    }

    private static Optional<String> worded(Set<ConstraintViolation<Account>> violations) {
        return violations.isEmpty() ? Optional.empty()
            : Optional.of(violations.stream().map(ConstraintViolation::getMessage).sorted().collect(Collectors.joining(", ")));
    }
}
