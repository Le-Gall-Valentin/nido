package com.boilerplate.api.authentication.infrastructure.security;

import com.boilerplate.api.shared.model.Role;

import java.util.UUID;

public record UserClaims(UUID userId, Role role, String email) {}