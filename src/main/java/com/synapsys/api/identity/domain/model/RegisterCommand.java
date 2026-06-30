package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;
import java.util.Locale;
import java.util.Objects;

public record RegisterCommand(String username, String email, String rawPassword, Role role) {
    public RegisterCommand {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(rawPassword, "rawPassword");
        Objects.requireNonNull(role, "role");
        email = email == null ? null : email.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "RegisterCommand[username=" + username + ", email=" + email + ", role=" + role + "]";
    }
}