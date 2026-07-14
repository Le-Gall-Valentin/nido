package com.nido.api.identity.domain.model;

import com.nido.api.shared.model.Role;

public record CreateUserProfileCommand(String username, String email, Role role) {}