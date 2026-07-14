package com.nido.api.authentication.infrastructure.security;

import com.nido.api.shared.model.Role;

import java.util.UUID;

public record UserClaims(UUID userId, Role role, String email) {}