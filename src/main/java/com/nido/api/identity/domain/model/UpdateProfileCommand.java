package com.nido.api.identity.domain.model;

import java.util.UUID;

/** @param emailCode the code sent to the new address, when the account's second factor is the mail; else null */
public record UpdateProfileCommand(UUID userId, String username, String email, String currentPassword, String emailCode) {
    public UpdateProfileCommand {
        username = new Username(username).value();
        email = EmailAddress.normalize(email);
    }

    public UpdateProfileCommand(UUID userId, String username, String email, String currentPassword) {
        this(userId, username, email, currentPassword, null);
    }

    @Override
    public String toString() {
        return "UpdateProfileCommand[userId=" + userId + ", username=" + username + ", email=***, currentPassword=***, emailCode=***]";
    }
}
