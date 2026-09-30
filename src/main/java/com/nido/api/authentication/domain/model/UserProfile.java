package com.nido.api.authentication.domain.model;

import com.nido.api.shared.model.Language;
import com.nido.api.shared.model.Role;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(UUID id, String username, String email, boolean isActive, Role role, Instant createdAt,
                          Language language) {

    /** Without a language (null: the account never recorded one). */
    public UserProfile(UUID id, String username, String email, boolean isActive, Role role, Instant createdAt) {
        this(id, username, email, isActive, role, createdAt, null);
    }
}