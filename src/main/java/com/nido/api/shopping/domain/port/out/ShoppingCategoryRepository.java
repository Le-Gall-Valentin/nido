package com.nido.api.shopping.domain.port.out;

import com.nido.api.shopping.domain.model.ShoppingCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingCategoryRepository {
    List<ShoppingCategory> findBySpaceId(UUID spaceId);
    boolean existsBySpaceId(UUID spaceId);
    ShoppingCategory create(UUID spaceId, String name, boolean fallback);
    Optional<ShoppingCategory> findById(UUID categoryId);
    ShoppingCategory rename(UUID categoryId, String name);
    void delete(UUID categoryId);
}
