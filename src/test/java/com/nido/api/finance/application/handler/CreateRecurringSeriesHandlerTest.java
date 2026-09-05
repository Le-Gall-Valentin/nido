package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRecurringSeriesHandlerTest {

    @Mock RecurringTransactionSeriesRepository seriesRepository;
    private CreateRecurringSeriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreateRecurringSeriesHandler(seriesRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_create_a_recurring_series_with_no_contributors() {
        CreateRecurringSeriesCommand command = new CreateRecurringSeriesCommand(spaceId, "Loyer", new BigDecimal("800.00"),
            TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        RecurringTransactionSeries created = new RecurringTransactionSeries(UUID.randomUUID(), spaceId, "Loyer",
            new BigDecimal("800.00"), TransactionType.EXPENSE, command.categoryId(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, null);
        when(seriesRepository.create(command, List.of())).thenReturn(created);

        RecurringTransactionSeries result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void resolves_an_equal_split_before_calling_the_repository() {
        CreateRecurringSeriesCommand command = new CreateRecurringSeriesCommand(spaceId, "Loyer", new BigDecimal("800.00"),
            TransactionType.EXPENSE, UUID.randomUUID(), aliceId,
            List.of(new ContributionInput(aliceId, null), new ContributionInput(bobId, null)),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        List<Contribution> expectedResolved = List.of(new Contribution(aliceId, new BigDecimal("400.00")), new Contribution(bobId, new BigDecimal("400.00")));
        RecurringTransactionSeries created = new RecurringTransactionSeries(UUID.randomUUID(), spaceId, "Loyer",
            new BigDecimal("800.00"), TransactionType.EXPENSE, command.categoryId(), aliceId, expectedResolved,
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, null);
        when(seriesRepository.create(command, expectedResolved)).thenReturn(created);

        RecurringTransactionSeries result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
        verify(seriesRepository).create(command, expectedResolved);
    }

    @Test
    void creating_a_recurring_series_with_custom_shares_that_do_not_sum_to_the_amount_is_rejected() {
        CreateRecurringSeriesCommand command = new CreateRecurringSeriesCommand(spaceId, "Loyer", new BigDecimal("800.00"),
            TransactionType.EXPENSE, UUID.randomUUID(), aliceId,
            List.of(new ContributionInput(aliceId, new BigDecimal("500.00")), new ContributionInput(bobId, new BigDecimal("200.00"))),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.InvalidContributionShares.class);
        verify(seriesRepository, never()).create(any(), any());
    }

    @Test
    void a_viewer_cannot_create_a_recurring_series() {
        CreateRecurringSeriesCommand command = new CreateRecurringSeriesCommand(spaceId, "Loyer", new BigDecimal("800.00"),
            TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
