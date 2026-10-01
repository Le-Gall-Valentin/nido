package com.nido.api.identity.domain.model;

import com.nido.api.shared.model.Role;

/**
 * The account as it reaches the database: the username is one Nido accepts and the address is kept as
 * Nido keeps addresses — whoever builds the command, the request, the seed or a test.
 */
public record CreateUserProfileCommand(String username, String email, Role role) {
    public CreateUserProfileCommand {
        username = new Username(username).value();
        email = EmailAddress.normalize(email);
    }
}