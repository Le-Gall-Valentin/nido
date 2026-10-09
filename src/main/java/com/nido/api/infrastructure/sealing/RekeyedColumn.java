package com.nido.api.infrastructure.sealing;

import com.nido.api.infrastructure.encryption.CurrentOrLegacyTextEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.Objects;
import java.util.function.Function;

/**
 * A column encrypted under a key of its own rather than a space's — two-factor secrets, the SMTP password, the mail
 * queue — whose values versions up to 0.15.x wrote with the legacy key. Declared by the module that owns it, with the
 * encryptor of each row ({@link CurrentOrLegacyTextEncryptor}); RekeyMigration brings every value to the current key.
 */
public final class RekeyedColumn {

    private final String table;
    private final String column;
    private final String rowKey;
    private final String rows;
    private final Function<String, TextEncryptor> encryptorForRow;
    private final boolean deleteUnreadable;

    private RekeyedColumn(String table, String column, String rowKey, String rows,
                          Function<String, TextEncryptor> encryptorForRow, boolean deleteUnreadable) {
        this.table = table;
        this.column = column;
        this.rowKey = rowKey;
        this.rows = rows;
        this.encryptorForRow = encryptorForRow;
        this.deleteUnreadable = deleteUnreadable;
    }

    /**
     * @param rowKey the column that tells the rows apart, among those {@code rows} selects
     * @param rows   a constant SQL condition written in the code, never built from input; null for every row
     * @param encryptorForRow the encryptor of a row, from its key as text
     */
    public static RekeyedColumn of(String table, String column, String rowKey, String rows,
                                   Function<String, TextEncryptor> encryptorForRow) {
        return new RekeyedColumn(SqlIdentifier.require(table), SqlIdentifier.require(column), SqlIdentifier.require(rowKey),
            rows == null ? "TRUE" : rows, Objects.requireNonNull(encryptorForRow, "encryptorForRow"), false);
    }

    /** A row whose value no key opens is deleted instead of stopping the start — the mail queue, whose dispatcher drops it anyway. */
    public RekeyedColumn deletingUnreadableRows() {
        return new RekeyedColumn(table, column, rowKey, rows, encryptorForRow, true);
    }

    public String table() {
        return table;
    }

    public String column() {
        return column;
    }

    String rowKey() {
        return rowKey;
    }

    String rows() {
        return rows;
    }

    TextEncryptor encryptorFor(String key) {
        return encryptorForRow.apply(key);
    }

    boolean deletesUnreadable() {
        return deleteUnreadable;
    }

    @Override
    public String toString() {
        return table + "." + column;
    }
}
