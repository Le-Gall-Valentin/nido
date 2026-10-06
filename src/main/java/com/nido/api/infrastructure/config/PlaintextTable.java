package com.nido.api.infrastructure.config;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A table some of whose text columns go from clear to encrypted: each {@code <column>} is emptied as
 * {@code <column>_encrypted} is filled. The SQL is assembled from these names, so they are checked to be
 * plain identifiers — they come from code, never from a request.
 *
 * @param spaceOf the SQL expression giving a row's space, over the table aliased {@code t} and its {@code join}
 */
public record PlaintextTable(String name, List<String> columns, String spaceOf, String join) {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z_]*");

    public PlaintextTable {
        identifier(name);
        columns = List.copyOf(columns);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException(name + " has no column to encrypt");
        }
        columns.forEach(PlaintextTable::identifier);
        Objects.requireNonNull(spaceOf, "spaceOf");
        Objects.requireNonNull(join, "join");
    }

    /** A table with a space_id of its own. */
    public static PlaintextTable ofSpace(String name, String... columns) {
        return new PlaintextTable(name, List.of(columns), "t.space_id", "");
    }

    /** A table whose rows belong to a parent row carrying the space_id: a subtask to its task. */
    public static PlaintextTable throughParent(String name, String parentForeignKey, String parentTable, String... columns) {
        identifier(parentForeignKey);
        identifier(parentTable);
        return new PlaintextTable(name, List.of(columns), "p.space_id",
            "JOIN " + parentTable + " p ON p.id = t." + parentForeignKey);
    }

    /** The spaces table: a row is its own space. */
    public static PlaintextTable ofSpaceItself(String name, String... columns) {
        return new PlaintextTable(name, List.of(columns), "t.id", "");
    }

    private static void identifier(String value) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Not a plain SQL identifier: " + value);
        }
    }
}
