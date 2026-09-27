package com.nido.api.dashboard.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** The shopping card: what is left to buy, aisle by aisle in the list's own order. No card once the list is done. */
public final class ShoppingAisles {

    /** Item names shown per aisle; the count still sees every item. */
    public static final int PREVIEW_SIZE = 3;

    /** An item of the shopping list, in the aisle it was filed under. */
    public record Item(String name, UUID aisleId, int position, boolean done) {
    }

    /** An aisle of the shopping list; the fallback one takes the items whose aisle is gone. */
    public record Aisle(UUID id, String name, int position, boolean fallback) {
    }

    private ShoppingAisles() {}

    /**
     * @param aisles read only when something is left to buy — reading them may do more than read (the
     *               shopping module seeds its default aisles on first read)
     */
    public static SourceResult of(List<Item> items, Supplier<List<Aisle>> aisles) {
        List<Item> leftToBuy = items.stream()
            .filter(item -> !item.done())
            .sorted(Comparator.comparingInt(Item::position))
            .toList();
        if (leftToBuy.isEmpty()) {
            return SourceResult.nothing();
        }

        List<Aisle> ordered = aisles.get().stream().sorted(Comparator.comparingInt(Aisle::position)).toList();
        Set<UUID> known = ordered.stream().map(Aisle::id).collect(Collectors.toSet());
        UUID fallbackId = ordered.stream().filter(Aisle::fallback).map(Aisle::id).findFirst().orElse(null);

        Map<UUID, List<Item>> byAisle = new HashMap<>();
        for (Item item : leftToBuy) {
            UUID aisleId = known.contains(item.aisleId()) ? item.aisleId() : fallbackId;
            if (aisleId != null) {
                byAisle.computeIfAbsent(aisleId, id -> new ArrayList<>()).add(item);
            }
        }

        List<ShoppingGroup> groups = ordered.stream()
            .filter(aisle -> byAisle.containsKey(aisle.id()))
            .map(aisle -> {
                List<Item> filed = byAisle.get(aisle.id());
                return new ShoppingGroup(aisle.id(), aisle.name(), filed.size(),
                    filed.stream().limit(PREVIEW_SIZE).map(Item::name).toList());
            })
            .toList();
        return SourceResult.of(new ShoppingCard(leftToBuy.size(), groups));
    }
}
