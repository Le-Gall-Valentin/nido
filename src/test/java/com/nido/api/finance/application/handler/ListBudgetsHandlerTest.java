package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListBudgetsHandlerTest {

    @Mock BudgetRepository budgetRepository;
    private ListBudgetsHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListBudgetsHandler(budgetRepository);
    }

    @Test
    void lists_every_budget_in_the_callers_space() {
        Budget budget = new Budget(UUID.randomUUID(), spaceId, UUID.randomUUID(), new BigDecimal("450.00"));
        when(budgetRepository.findBySpaceId(spaceId)).thenReturn(List.of(budget));
        SpaceMembership membership = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        List<Budget> result = handler.list(membership);

        assertThat(result).containsExactly(budget);
    }
}
