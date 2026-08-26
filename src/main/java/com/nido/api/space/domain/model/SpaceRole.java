package com.nido.api.space.domain.model;

public enum SpaceRole {
    OWNER, ADMIN, MEMBER, VIEWER;

    /** Rang croissant en pouvoir : VIEWER < MEMBER < ADMIN < OWNER. */
    public int rank() {
        return switch (this) {
            case VIEWER -> 0;
            case MEMBER -> 1;
            case ADMIN -> 2;
            case OWNER -> 3;
        };
    }

    public boolean atLeast(SpaceRole required) {
        return rank() >= required.rank();
    }

    /** VIEWER est en lecture seule : toute écriture métier exige au moins MEMBER. */
    public boolean canWrite() {
        return this != VIEWER;
    }

    /** Gestion des membres, des invitations et de l'identité du groupe. */
    public boolean canManageSpace() {
        return this == OWNER || this == ADMIN;
    }
}
