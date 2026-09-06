package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.UpdateCategoryCommand;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.infrastructure.config.FinanceEncryptorFactory;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceCategoryEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceCategoryJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final FinanceCategoryJpaRepository categories;
    private final FinanceEncryptorFactory encryptorFactory;

    public CategoryRepositoryAdapter(FinanceCategoryJpaRepository categories, FinanceEncryptorFactory encryptorFactory) {
        this.categories = categories;
        this.encryptorFactory = encryptorFactory;
    }

    @Override
    public List<Category> findBySpaceId(UUID spaceId) {
        return categories.findBySpaceId(spaceId).stream().map(e -> toDomain(e, spaceId)).toList();
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
        return categories.findById(categoryId).map(e -> toDomain(e, e.getSpaceId()));
    }

    @Override
    @Transactional
    public Category create(CreateCategoryCommand command, boolean isDefault) {
        FinanceCategoryEntity e = new FinanceCategoryEntity();
        e.setSpaceId(command.spaceId());
        e.setDefault(isDefault);
        applyLabel(e, command.spaceId(), command.label(), isDefault);
        e.setColor(command.color());
        e.setIcon(command.icon());
        e.setType(command.type());
        FinanceCategoryEntity saved = categories.saveAndFlush(e);
        return toDomain(saved, command.spaceId());
    }

    @Override
    @Transactional
    public Category update(UpdateCategoryCommand command) {
        FinanceCategoryEntity e = categories.findById(command.categoryId()).orElseThrow(FinanceException.CategoryNotFound::new);
        applyLabel(e, command.spaceId(), command.label(), e.isDefault());
        e.setColor(command.color());
        e.setIcon(command.icon());
        FinanceCategoryEntity saved = categories.saveAndFlush(e);
        return toDomain(saved, command.spaceId());
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

    private void applyLabel(FinanceCategoryEntity e, UUID spaceId, String label, boolean isDefault) {
        if (isDefault) {
            e.setLabel(label);
            e.setLabelEncrypted(null);
        } else {
            e.setLabelEncrypted(encryptorFactory.forSpace(spaceId).encrypt(label));
            e.setLabel(null);
        }
    }

    private Category toDomain(FinanceCategoryEntity e, UUID spaceId) {
        String label = e.isDefault() ? e.getLabel() : encryptorFactory.forSpace(spaceId).decrypt(e.getLabelEncrypted());
        return new Category(e.getId(), e.getSpaceId(), label, e.getColor(), e.getIcon(), e.isDefault(), e.getType());
    }
}
