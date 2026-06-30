package com.synapsys.api.identity.domain.model;

import java.util.UUID;

public record UpdateProfileCommand(UUID userId, String username, String email) {

    @Override
    public String toString() {
        return "UpdateProfileCommand[userId=" + userId + ", username=" + username + ", email=***]";
    }
}