package com.nido.api.space.domain.model;

import java.util.Locale;

/**
 * Un code vide est refusé avec {@link SpaceException.InvitationNotFound}, exactement comme
 * un code inconnu : les deux ne doivent pas se distinguer.
 */
public record AcceptInvitationCommand(String code) {

    public AcceptInvitationCommand {
        code = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new SpaceException.InvitationNotFound();
        }
    }
}
