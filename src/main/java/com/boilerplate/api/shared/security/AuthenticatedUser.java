package com.boilerplate.api.shared.security;

import com.boilerplate.api.shared.model.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, Role role, String email) {}