package com.nido.api.finance.domain.model;

/** Where a member stands in an operation someone changed: still in it, newly in it, or no longer. */
public enum Involvement {
    CHANGED, ADDED, REMOVED;

    public static Involvement of(boolean concernedBefore, boolean concernedAfter) {
        if (concernedBefore && concernedAfter) {
            return CHANGED;
        }
        if (concernedAfter) {
            return ADDED;
        }
        if (concernedBefore) {
            return REMOVED;
        }
        throw new IllegalArgumentException("A member concerned neither before nor after a change has no part in it");
    }
}
