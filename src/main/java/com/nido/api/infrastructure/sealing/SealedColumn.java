package com.nido.api.infrastructure.sealing;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A column whose values are sealed — see {@link SpaceSealer}. Declared once, in its entity, next to its mapping;
 * the migration and the key check read the same declarations instead of spelling the names again. Built by its
 * factories only, from names checked to be plain identifiers.
 */
public final class SealedColumn {

    private static final Pattern REFERENCE = Pattern.compile("[a-z][a-z_]*\\.[a-z][a-z_]*:"
        + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final String table;
    private final String column;
    private final String spaceOf;
    private final String join;
    private final String clearColumn;

    private SealedColumn(String table, String column, String spaceOf, String join, String clearColumn) {
        this.table = table;
        this.column = column;
        this.spaceOf = spaceOf;
        this.join = join;
        this.clearColumn = clearColumn;
    }

    /** A table with a space_id of its own. */
    public static SealedColumn ofSpace(String table, String column) {
        return new SealedColumn(SqlIdentifier.require(table), SqlIdentifier.require(column), "t.space_id", "", null);
    }

    /** A table whose rows belong to a parent row carrying the space_id: a subtask to its task. */
    public static SealedColumn throughParent(String table, String column, String parentForeignKey, String parentTable) {
        return new SealedColumn(SqlIdentifier.require(table), SqlIdentifier.require(column), "p.space_id",
            "JOIN " + SqlIdentifier.require(parentTable) + " p ON p.id = t." + SqlIdentifier.require(parentForeignKey), null);
    }

    /** The spaces table: a row is its own space. */
    public static SealedColumn ofSpaceItself(String table, String column) {
        return new SealedColumn(SqlIdentifier.require(table), SqlIdentifier.require(column), "t.id", "", null);
    }

    /** The column that held the value in clear before 0.14.0: emptied by the migration, dropped by 068. */
    public SealedColumn withClearColumn(String clearColumn) {
        return new SealedColumn(table, column, spaceOf, join, SqlIdentifier.require(clearColumn));
    }

    public String table() {
        return table;
    }

    public String column() {
        return column;
    }

    public Optional<String> clearColumn() {
        return Optional.ofNullable(clearColumn);
    }

    public String referenceFor(UUID rowId) {
        return table + "." + column + ":" + Objects.requireNonNull(rowId, "rowId");
    }

    /** Whether a text has the shape of a {@link #referenceFor reference}: what only a sealing could have written. */
    static boolean isReference(String text) {
        return REFERENCE.matcher(text).matches();
    }

    /** The SQL expression giving a row's space, over the table aliased {@code t} and its {@link #join()}. */
    String spaceOf() {
        return spaceOf;
    }

    String join() {
        return join;
    }

    @Override
    public String toString() {
        return table + "." + column;
    }
}
