package com.boilerplate.api.authentication.domain.model;

import com.boilerplate.api.shared.model.Role;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(UUID id, String username, String email, boolean isActive, Role role, Instant createdAt) {}