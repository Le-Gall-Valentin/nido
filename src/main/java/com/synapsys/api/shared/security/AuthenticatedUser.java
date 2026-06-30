package com.synapsys.api.shared.security;

import com.synapsys.api.shared.model.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, Role role, String email) {}