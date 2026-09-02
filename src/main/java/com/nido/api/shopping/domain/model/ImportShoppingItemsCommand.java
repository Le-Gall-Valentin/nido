package com.nido.api.shopping.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ImportShoppingItemsCommand(UUID spaceId, List<ShoppingImportLine> lines) {
    public ImportShoppingItemsCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(lines, "lines");
    }
}
