package com.nido.api.space.domain.model;

/** Why a pending invitation stopped being valid — what its invitee is told. */
public enum InvitationCancellation {
    /** A manager of the space revoked it. */
    REVOKED,
    /** The owner deleted the space. */
    SPACE_DELETED,
    /** The owner's account was deleted and nobody could inherit the space, which went with it. */
    OWNER_LEFT_NIDO
}
