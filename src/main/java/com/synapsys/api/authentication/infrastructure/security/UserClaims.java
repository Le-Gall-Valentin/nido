package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.shared.model.Role;

import java.util.UUID;

public record UserClaims(UUID userId, Role role, String email) {}