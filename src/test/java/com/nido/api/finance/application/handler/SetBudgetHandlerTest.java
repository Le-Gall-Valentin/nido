package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SetBudgetCommand;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetBudgetHandlerTest {

    @Mock BudgetRepository budgetRepository;
    @Mock CategoryRepository categoryRepository;
    private SetBudgetHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new SetBudgetHandler(budgetRepository, categoryRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_set_a_budget() {
        SetBudgetCommand command = new SetBudgetCommand(spaceId, UUID.randomUUID(), new BigDecimal("450.00"));
        Category category = new Category(command.categoryId(), spaceId, "Alimentation", "#f59e0b", "Utensils", true, TransactionType.EXPENSE);
        Budget saved = new Budget(UUID.randomUUID(), spaceId, command.categoryId(), command.monthlyLimit());
        when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(category));
        when(budgetRepository.upsert(command)).thenReturn(saved);

        Budget result = handler.set(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void a_viewer_cannot_set_a_budget() {
        SetBudgetCommand command = new SetBudgetCommand(spaceId, UUID.randomUUID(), new BigDecimal("450.00"));

        assertThatThrownBy(() -> handler.set(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void setting_a_budget_for_another_space_than_the_callers_is_rejected() {
        SetBudgetCommand command = new SetBudgetCommand(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("450.00"));

        assertThatThrownBy(() -> handler.set(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
    }

    @Test
    void setting_a_budget_for_a_category_that_belongs_to_a_different_space_is_rejected() {
        UUID otherSpaceId = UUID.randomUUID();
        SetBudgetCommand command = new SetBudgetCommand(spaceId, UUID.randomUUID(), new BigDecimal("450.00"));
        Category category = new Category(command.categoryId(), otherSpaceId, "Alimentation", "#f59e0b", "Utensils", true, TransactionType.EXPENSE);
        when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> handler.set(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.CategoryNotFound.class);
    }

    @Test
    void setting_a_budget_for_a_category_that_does_not_exist_is_rejected() {
        SetBudgetCommand command = new SetBudgetCommand(spaceId, UUID.randomUUID(), new BigDecimal("450.00"));
        when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.set(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.CategoryNotFound.class);
    }

    @Test
    void setting_a_budget_on_an_income_category_is_rejected() {
        SetBudgetCommand command = new SetBudgetCommand(spaceId, UUID.randomUUID(), new BigDecimal("450.00"));
        Category category = new Category(command.categoryId(), spaceId, "Revenu", "#22c55e", "Wallet", true, TransactionType.INCOME);
        when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> handler.set(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.BudgetRequiresExpenseCategory.class);
    }
}
