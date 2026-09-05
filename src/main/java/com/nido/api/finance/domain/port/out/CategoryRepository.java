package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.UpdateCategoryCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    List<Category> findBySpaceId(UUID spaceId);
    boolean existsBySpaceId(UUID spaceId);
    Optional<Category> findById(UUID categoryId);
    Category create(CreateCategoryCommand command, boolean isDefault);
    Category update(UpdateCategoryCommand command);
    void delete(UUID categoryId);
    boolean isReferencedByTransactions(UUID categoryId);
}
