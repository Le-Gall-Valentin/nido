package com.nido.api.dashboard.domain.model;

import java.util.List;
import java.util.UUID;

/** {@code preview} holds the first three item names, in list order. */
public record ShoppingGroup(UUID categoryId, String name, int count, List<String> preview) {
}
