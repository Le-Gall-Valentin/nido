package com.nido.api.instance.domain.port.out;

import java.util.Optional;

/**
 * What the setup screen requires of the first administrator's address and password, for an account
 * that does not go through it — NIDO_SEED_*. One definition, kept where requests are checked.
 */
public interface AccountRulesPort {

    /** What is wrong with the address, worded for a log — never quoting it. */
    Optional<String> emailProblem(String email);

    /** What is wrong with the password, worded for a log — never quoting it. */
    Optional<String> passwordProblem(String password);
}
