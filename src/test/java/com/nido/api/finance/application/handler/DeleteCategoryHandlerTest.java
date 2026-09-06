package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteCategoryHandlerTest {

    @Mock CategoryRepository categoryRepository;
    @Mock BudgetRepository budgetRepository;
    private DeleteCategoryHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteCategoryHandler(categoryRepository, budgetRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_delete_an_unused_category_and_its_budget_is_deleted_too() {
        Category existing = new Category(categoryId, spaceId, "Divers", "#64748b", "MoreHorizontal", false, TransactionType.EXPENSE);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.isReferencedByTransactions(categoryId)).thenReturn(false);

        handler.delete(categoryId, spaceId, membership(SpaceRole.MEMBER));

        verify(budgetRepository).deleteByCategoryId(categoryId);
        verify(categoryRepository).delete(categoryId);
    }

    @Test
    void deleting_a_category_still_used_by_a_transaction_is_rejected() {
        Category existing = new Category(categoryId, spaceId, "Divers", "#64748b", "MoreHorizontal", false, TransactionType.EXPENSE);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.isReferencedByTransactions(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> handler.delete(categoryId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.CategoryInUse.class);
    }

    @Test
    void a_viewer_cannot_delete_a_category() {
        assertThatThrownBy(() -> handler.delete(categoryId, spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
