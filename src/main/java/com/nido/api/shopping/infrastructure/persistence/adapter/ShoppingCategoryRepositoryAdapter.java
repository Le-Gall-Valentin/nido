package com.nido.api.shopping.infrastructure.persistence.adapter;

import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shopping.infrastructure.persistence.entity.ShoppingCategoryEntity;
import com.nido.api.shopping.infrastructure.persistence.repository.ShoppingCategoryJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ShoppingCategoryRepositoryAdapter implements ShoppingCategoryRepository {

    private final ShoppingCategoryJpaRepository categories;

    public ShoppingCategoryRepositoryAdapter(ShoppingCategoryJpaRepository categories) {
        this.categories = categories;
    }

    @Override
    public List<ShoppingCategory> findBySpaceId(UUID spaceId) {
        return categories.findBySpaceIdOrderByPositionAsc(spaceId).stream()
            .map(ShoppingCategoryRepositoryAdapter::toDomain).toList();
    }

    @Override
    public boolean existsBySpaceId(UUID spaceId) {
        return categories.existsBySpaceId(spaceId);
    }

    @Override
    @Transactional
    public ShoppingCategory create(UUID spaceId, String name, boolean fallback) {
        ShoppingCategoryEntity e = new ShoppingCategoryEntity();
        e.setSpaceId(spaceId);
        e.setName(name);
        e.setFallback(fallback);
        e.setPosition((int) categories.countBySpaceId(spaceId));
        return toDomain(categories.saveAndFlush(e));
    }

    @Override
    public Optional<ShoppingCategory> findById(UUID categoryId) {
        return categories.findById(categoryId).map(ShoppingCategoryRepositoryAdapter::toDomain);
    }

    @Override
    public ShoppingCategory rename(UUID categoryId, String name) {
        ShoppingCategoryEntity e = categories.findById(categoryId).orElseThrow(ShoppingException.CategoryNotFound::new);
        e.setName(name);
        return toDomain(categories.saveAndFlush(e));
    }

    @Override
    public void delete(UUID categoryId) {
        categories.deleteById(categoryId);
        categories.flush();
    }

    private static ShoppingCategory toDomain(ShoppingCategoryEntity e) {
        return new ShoppingCategory(e.getId(), e.getSpaceId(), e.getName(), e.getPosition(), e.isFallback());
    }
}
