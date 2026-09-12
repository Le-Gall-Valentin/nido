package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.SetBudgetCommand;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceBudgetJpaRepository;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class BudgetRepositoryAdapterIT {

    @Autowired BudgetRepositoryAdapter adapter;
    @Autowired FinanceBudgetJpaRepository jpaRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired CategoryRepositoryAdapter categoryAdapter;

    private UUID spaceId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        spaceJpaRepository.deleteAll();
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();
        // finance_budgets.category_id has a foreign key onto finance_categories(id) — a
        // real category row is required, a bare random UUID violates the constraint.
        Category category = categoryAdapter.create(new CreateCategoryCommand(spaceId, "Alimentation", "#f59e0b", "Utensils", TransactionType.EXPENSE), true);
        categoryId = category.id();
    }

    @Test
    void upsert_creates_the_budget_with_an_encrypted_limit_at_rest() {
        Budget created = adapter.upsert(new SetBudgetCommand(spaceId, categoryId, new BigDecimal("450.00")));

        assertThat(created.monthlyLimit()).isEqualByComparingTo("450.00");
        String rawStoredValue = jpaRepository.findByCategoryId(categoryId).orElseThrow().getMonthlyLimitEncrypted();
        assertThat(rawStoredValue).isNotNull().doesNotContain("450.00");
    }

    @Test
    void upsert_updates_the_existing_row_instead_of_creating_a_duplicate() {
        adapter.upsert(new SetBudgetCommand(spaceId, categoryId, new BigDecimal("450.00")));

        Budget updated = adapter.upsert(new SetBudgetCommand(spaceId, categoryId, new BigDecimal("500.00")));

        assertThat(updated.monthlyLimit()).isEqualByComparingTo("500.00");
        assertThat(adapter.findBySpaceId(spaceId)).hasSize(1);
    }

    @Test
    void findByCategoryId_decrypts_the_stored_limit() {
        adapter.upsert(new SetBudgetCommand(spaceId, categoryId, new BigDecimal("123.45")));

        assertThat(adapter.findByCategoryId(categoryId).orElseThrow().monthlyLimit()).isEqualByComparingTo("123.45");
    }

    @Test
    void deleteByCategoryId_removes_the_budget() {
        adapter.upsert(new SetBudgetCommand(spaceId, categoryId, new BigDecimal("450.00")));

        adapter.deleteByCategoryId(categoryId);

        assertThat(adapter.findByCategoryId(categoryId)).isEmpty();
    }
}
