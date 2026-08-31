package com.nido.api.shopping.infrastructure.persistence.adapter;

import com.nido.api.shopping.domain.model.AddShoppingItemCommand;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.model.UpdateShoppingItemCommand;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shopping.infrastructure.persistence.entity.ShoppingItemEntity;
import com.nido.api.shopping.infrastructure.persistence.repository.ShoppingItemJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ShoppingItemRepositoryAdapter implements ShoppingItemRepository {

    private final ShoppingItemJpaRepository items;

    public ShoppingItemRepositoryAdapter(ShoppingItemJpaRepository items) {
        this.items = items;
    }

    @Override
    public List<ShoppingItem> findBySpaceId(UUID spaceId) {
        return items.findBySpaceIdOrderByPositionAsc(spaceId).stream().map(ShoppingItemRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<ShoppingItem> findBySpaceIdAndDoneFalse(UUID spaceId) {
        return items.findBySpaceIdAndDoneFalseOrderByPositionAsc(spaceId).stream()
            .map(ShoppingItemRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<ShoppingItem> findById(UUID itemId) {
        return items.findById(itemId).map(ShoppingItemRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public ShoppingItem add(AddShoppingItemCommand command) {
        ShoppingItemEntity e = new ShoppingItemEntity();
        e.setSpaceId(command.spaceId());
        e.setCategoryId(command.categoryId());
        e.setName(command.name());
        e.setQuantityLabel(command.quantityLabel());
        e.setDone(false);
        e.setPosition((int) items.countBySpaceIdAndCategoryId(command.spaceId(), command.categoryId()));
        return toDomain(items.saveAndFlush(e));
    }

    @Override
    public ShoppingItem update(UpdateShoppingItemCommand command) {
        ShoppingItemEntity e = items.findById(command.itemId()).orElseThrow(ShoppingException.ItemNotFound::new);
        e.setCategoryId(command.categoryId());
        e.setName(command.name());
        e.setQuantityLabel(command.quantityLabel());
        return toDomain(items.saveAndFlush(e));
    }

    @Override
    public void toggleDone(UUID itemId) {
        ShoppingItemEntity e = items.findById(itemId).orElseThrow(ShoppingException.ItemNotFound::new);
        e.setDone(!e.isDone());
        items.saveAndFlush(e);
    }

    @Override
    public void delete(UUID itemId) {
        items.deleteById(itemId);
        items.flush();
    }

    @Override
    @Transactional
    public void deleteDoneBySpaceId(UUID spaceId) {
        items.deleteBySpaceIdAndDoneTrue(spaceId);
    }

    @Override
    @Transactional
    public void deleteAllBySpaceId(UUID spaceId) {
        items.deleteBySpaceId(spaceId);
    }

    @Override
    @Transactional
    public void reassignCategory(UUID fromCategoryId, UUID toCategoryId) {
        items.reassignCategory(fromCategoryId, toCategoryId);
    }

    private static ShoppingItem toDomain(ShoppingItemEntity e) {
        return new ShoppingItem(e.getId(), e.getSpaceId(), e.getCategoryId(), e.getName(), e.getQuantityLabel(), e.isDone(), e.getPosition());
    }
}
