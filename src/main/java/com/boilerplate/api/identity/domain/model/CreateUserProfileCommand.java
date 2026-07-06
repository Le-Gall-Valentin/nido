package com.boilerplate.api.identity.domain.model;

import com.boilerplate.api.shared.model.Role;

public record CreateUserProfileCommand(String username, String email, Role role) {}