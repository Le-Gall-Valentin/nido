package com.nido.api.shared.security;

import com.nido.api.shared.model.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, Role role, String email) {}