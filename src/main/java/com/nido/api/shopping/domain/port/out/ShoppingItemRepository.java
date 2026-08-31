package com.nido.api.shopping.domain.port.out;

import com.nido.api.shopping.domain.model.AddShoppingItemCommand;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.model.UpdateShoppingItemCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingItemRepository {
    List<ShoppingItem> findBySpaceId(UUID spaceId);
    List<ShoppingItem> findBySpaceIdAndDoneFalse(UUID spaceId);
    Optional<ShoppingItem> findById(UUID itemId);
    ShoppingItem add(AddShoppingItemCommand command);
    ShoppingItem update(UpdateShoppingItemCommand command);
    void toggleDone(UUID itemId);
    void delete(UUID itemId);
    void deleteDoneBySpaceId(UUID spaceId);
    void deleteAllBySpaceId(UUID spaceId);
    void reassignCategory(UUID fromCategoryId, UUID toCategoryId);
}
