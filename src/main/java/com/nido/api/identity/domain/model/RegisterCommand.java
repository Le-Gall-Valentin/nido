package com.nido.api.identity.domain.model;

import com.nido.api.shared.model.Role;
import java.util.Objects;

public record RegisterCommand(String username, String email, String rawPassword, Role role) {
    public RegisterCommand {
        username = new Username(username).value();
        Objects.requireNonNull(rawPassword, "rawPassword");
        Objects.requireNonNull(role, "role");
        email = EmailAddress.normalize(email);
    }

    @Override
    public String toString() {
        return "RegisterCommand[username=" + username + ", email=***, role=" + role + "]";
    }
}