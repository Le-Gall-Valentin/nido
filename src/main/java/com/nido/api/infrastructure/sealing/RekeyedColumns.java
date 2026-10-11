package com.nido.api.infrastructure.sealing;

import java.util.List;

/** The columns to re-encrypt a module owns, published as a bean: the migration reads them all. */
public record RekeyedColumns(List<RekeyedColumn> columns) {

    public RekeyedColumns {
        columns = List.copyOf(columns);
    }

    public static RekeyedColumns of(RekeyedColumn... columns) {
        return new RekeyedColumns(List.of(columns));
    }
}
