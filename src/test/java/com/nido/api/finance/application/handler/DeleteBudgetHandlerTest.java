package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.FinanceException;
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
class DeleteBudgetHandlerTest {

    @Mock BudgetRepository budgetRepository;
    @Mock CategoryRepository categoryRepository;
    private DeleteBudgetHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteBudgetHandler(budgetRepository, categoryRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Category category(UUID id, UUID inSpaceId) {
        return new Category(id, inSpaceId, "Alimentation", "#f59e0b", "Utensils", true);
    }

    @Test
    void a_member_can_delete_a_budget() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category(categoryId, spaceId)));

        handler.delete(spaceId, categoryId, membership(SpaceRole.MEMBER));

        verify(budgetRepository).deleteByCategoryId(categoryId);
    }

    @Test
    void a_viewer_cannot_delete_a_budget() {
        assertThatThrownBy(() -> handler.delete(spaceId, categoryId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void deleting_a_budget_for_another_space_than_the_callers_is_rejected() {
        assertThatThrownBy(() -> handler.delete(UUID.randomUUID(), categoryId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
    }

    @Test
    void deleting_a_budget_for_a_category_that_belongs_to_a_different_space_is_rejected() {
        UUID otherSpaceId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category(categoryId, otherSpaceId)));

        assertThatThrownBy(() -> handler.delete(spaceId, categoryId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.CategoryNotFound.class);
    }

    @Test
    void deleting_a_budget_for_a_category_that_does_not_exist_is_rejected() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.delete(spaceId, categoryId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.CategoryNotFound.class);
    }
}
