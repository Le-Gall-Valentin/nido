package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.UpdateCategoryCommand;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceCategoryEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceCategoryJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final FinanceCategoryJpaRepository categories;
    private final SpaceSealers sealers;

    public CategoryRepositoryAdapter(FinanceCategoryJpaRepository categories, SpaceSealers sealers) {
        this.categories = categories;
        this.sealers = sealers;
    }

    @Override
    public List<Category> findBySpaceId(UUID spaceId) {
        return categories.findBySpaceId(spaceId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsBySpaceId(UUID spaceId) {
        return categories.existsBySpaceId(spaceId);
    }

    @Override
    @Transactional
    public void lockForSeeding(UUID spaceId) {
        categories.lockForSeeding("finance-category-seed|" + spaceId);
    }

    @Override
    public Optional<Category> findById(UUID categoryId) {
        return categories.findById(categoryId).map(this::toDomain);
    }

    @Override
    @Transactional
    public Category create(CreateCategoryCommand command, boolean isDefault) {
        FinanceCategoryEntity e = new FinanceCategoryEntity();
        e.setSpaceId(command.spaceId());
        e.setDefault(isDefault);
        e.setLabelEncrypted(sealers.forSpace(command.spaceId()).seal(FinanceCategoryEntity.LABEL, e.getId(), command.label()));
        e.setColor(command.color());
        e.setIcon(command.icon());
        e.setType(command.type());
        FinanceCategoryEntity saved = categories.saveAndFlush(e);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public Category update(UpdateCategoryCommand command) {
        FinanceCategoryEntity e = categories.findById(command.categoryId()).orElseThrow(FinanceException.CategoryNotFound::new);
        e.setLabelEncrypted(sealers.forSpace(e.getSpaceId()).seal(FinanceCategoryEntity.LABEL, e.getId(), command.label()));
        e.setColor(command.color());
        e.setIcon(command.icon());
        FinanceCategoryEntity saved = categories.saveAndFlush(e);
        return toDomain(saved);
    }

    @Override
    public void delete(UUID categoryId) {
        categories.deleteById(categoryId);
        categories.flush();
    }

    @Override
    public boolean isReferencedByTransactions(UUID categoryId) {
        return categories.existsTransactionForCategory(categoryId);
    }

    private Category toDomain(FinanceCategoryEntity e) {
        String label = sealers.forSpace(e.getSpaceId()).open(FinanceCategoryEntity.LABEL, e.getId(), e.getLabelEncrypted());
        return new Category(e.getId(), e.getSpaceId(), label, e.getColor(), e.getIcon(), e.isDefault(), e.getType());
    }
}
