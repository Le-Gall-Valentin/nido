package com.nido.api.kitchen.domain.model;

public abstract sealed class KitchenException extends RuntimeException
    permits KitchenException.RecipeNotFound, KitchenException.MenuEntryNotFound {

    private KitchenException(String message) { super(message); }

    public static final class RecipeNotFound extends KitchenException {
        public RecipeNotFound() { super("Recipe not found"); }
    }

    public static final class MenuEntryNotFound extends KitchenException {
        public MenuEntryNotFound() { super("Menu entry not found"); }
    }
}
