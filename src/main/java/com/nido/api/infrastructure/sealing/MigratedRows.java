package com.nido.api.infrastructure.sealing;

/**
 * What a migration did to the values of a column, for the start's log: sealed — in clear or encrypted without an
 * envelope, as up to 0.13 —, re-encrypted — sealed or encrypted with the legacy key, as 0.14 to 0.15.x —, or dropped,
 * unreadable, by a column that deletes such rows.
 */
public record MigratedRows(int sealed, int reencrypted, int dropped) {

    public static final MigratedRows NONE = new MigratedRows(0, 0, 0);
    static final MigratedRows ONE_SEALED = new MigratedRows(1, 0, 0);
    static final MigratedRows ONE_REENCRYPTED = new MigratedRows(0, 1, 0);
    static final MigratedRows ONE_DROPPED = new MigratedRows(0, 0, 1);

    public int total() {
        return sealed + reencrypted + dropped;
    }

    MigratedRows plus(MigratedRows other) {
        return new MigratedRows(sealed + other.sealed, reencrypted + other.reencrypted, dropped + other.dropped);
    }
}
