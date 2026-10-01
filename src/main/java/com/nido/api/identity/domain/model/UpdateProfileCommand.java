package com.nido.api.identity.domain.model;

import java.util.UUID;

public record UpdateProfileCommand(UUID userId, String username, String email, String currentPassword) {
    public UpdateProfileCommand {
        username = new Username(username).value();
        email = EmailAddress.normalize(email);
    }

    @Override
    public String toString() {
        return "UpdateProfileCommand[userId=" + userId + ", username=" + username + ", email=***, currentPassword=***]";
    }
}