package com.nido.api.identity.domain.model;

import java.util.Locale;
import java.util.UUID;

public record UpdateProfileCommand(UUID userId, String username, String email) {
    public UpdateProfileCommand {
        email = email == null ? null : email.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "UpdateProfileCommand[userId=" + userId + ", username=" + username + ", email=***]";
    }
}