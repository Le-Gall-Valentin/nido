package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;

public record CreateUserProfileCommand(String username, String email, Role role) {}