package com.nido.api.shopping.domain.model;

import java.util.Objects;
import java.util.UUID;

/** One suggested line from Kitchen's computed weekly shopping list, already formatted and category-assigned by the frontend modal. */
public record ShoppingImportLine(String name, String quantityLabel, UUID categoryId) {
    public ShoppingImportLine {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(categoryId, "categoryId");
    }
}
