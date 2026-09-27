package com.nido.api.dashboard.domain.model;

import java.util.List;

public record ShoppingCard(int remaining, List<ShoppingGroup> categories) implements DashboardCard {

    public ShoppingCard {
        categories = List.copyOf(categories);
    }
}
