package com.nido.api.space.domain.model;

public enum SpaceRole {
    OWNER, ADMIN, MEMBER, VIEWER;

    /** VIEWER est en lecture seule : toute écriture métier exige au moins MEMBER. */
    public boolean canWrite() {
        return this != VIEWER;
    }

    /** Gestion des membres, des invitations et de l'identité du groupe. */
    public boolean canManageSpace() {
        return this == OWNER || this == ADMIN;
    }
}
