package com.nido.api.identity.domain.model;

/** An account just created, and how its invitation left. */
public record RegisteredAccount(User user, InvitationDelivery invitation) {
}
