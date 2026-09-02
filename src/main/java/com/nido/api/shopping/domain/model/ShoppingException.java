package com.nido.api.shopping.domain.model;

public abstract sealed class ShoppingException extends RuntimeException
    permits ShoppingException.CategoryNotFound, ShoppingException.ItemNotFound, ShoppingException.CannotDeleteFallbackCategory {

    private ShoppingException(String message) { super(message); }

    public static final class CategoryNotFound extends ShoppingException {
        public CategoryNotFound() { super("Shopping category not found"); }
    }

    public static final class ItemNotFound extends ShoppingException {
        public ItemNotFound() { super("Shopping item not found"); }
    }

    /** The fallback category always exists per space and absorbs a deleted category's items — it can never be deleted itself. */
    public static final class CannotDeleteFallbackCategory extends ShoppingException {
        public CannotDeleteFallbackCategory() { super("Cannot delete the fallback category"); }
    }
}
