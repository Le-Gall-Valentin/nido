package com.nido.api.infrastructure.sealing;

import java.util.List;

/** The sealed columns a module owns, published as a bean: the migration and the key check read them all. */
public record SealedColumns(List<SealedColumn> columns) {

    public SealedColumns {
        columns = List.copyOf(columns);
    }

    public static SealedColumns of(SealedColumn... columns) {
        return new SealedColumns(List.of(columns));
    }
}
