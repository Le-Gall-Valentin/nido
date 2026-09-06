package com.nido.api.shopping.domain.port.out;

import com.nido.api.shopping.domain.model.ShoppingCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingCategoryRepository {
    List<ShoppingCategory> findBySpaceId(UUID spaceId);
    boolean existsBySpaceId(UUID spaceId);
    /**
     * Serializes concurrent default-category seeding for a space: held until the caller's
     * transaction commits or rolls back, so two requests racing to seed the same new space
     * can't both pass {@code existsBySpaceId} and each insert the defaults.
     */
    void lockForSeeding(UUID spaceId);
    ShoppingCategory create(UUID spaceId, String name, boolean fallback);
    Optional<ShoppingCategory> findById(UUID categoryId);
    ShoppingCategory rename(UUID categoryId, String name);
    void delete(UUID categoryId);
}
