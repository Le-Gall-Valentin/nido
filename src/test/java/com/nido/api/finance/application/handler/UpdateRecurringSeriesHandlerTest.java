package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.service.SpaceMemberValidator;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.model.UpdateRecurringSeriesCommand;
import com.nido.api.finance.domain.port.out.CategoryRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRecurringSeriesHandlerTest {

    @Mock RecurringTransactionSeriesRepository seriesRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock SpaceMemberValidator spaceMemberValidator;
    private UpdateRecurringSeriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId = UUID.randomUUID();
    private final Category category = new Category(UUID.randomUUID(), spaceId, "Alimentation", "#f59e0b", "Utensils", true);

    @BeforeEach
    void setUp() {
        handler = new UpdateRecurringSeriesHandler(seriesRepository, categoryRepository, spaceMemberValidator);
        lenient().when(categoryRepository.findById(any())).thenReturn(Optional.of(category));
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private RecurringTransactionSeries existingInSameSpace(UUID seriesId) {
        return new RecurringTransactionSeries(seriesId, spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE,
            UUID.randomUUID(), null, List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, null);
    }

    @Test
    void a_member_can_update_a_recurring_series() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        when(seriesRepository.findById(command.seriesId())).thenReturn(Optional.of(existingInSameSpace(command.seriesId())));
        RecurringTransactionSeries updated = new RecurringTransactionSeries(command.seriesId(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, command.categoryId(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, null);
        when(seriesRepository.update(command, List.of())).thenReturn(updated);

        RecurringTransactionSeries result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void updating_a_recurring_series_that_belongs_to_a_different_space_is_rejected() {
        UUID seriesId = UUID.randomUUID();
        UUID otherSpaceId = UUID.randomUUID();
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(seriesId, spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        RecurringTransactionSeries foreign = new RecurringTransactionSeries(seriesId, otherSpaceId, "Loyer", new BigDecimal("800.00"),
            TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, null);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.RecurringSeriesNotFound.class);
        verify(seriesRepository, never()).update(any(), any());
    }

    @Test
    void updating_a_recurring_series_that_does_not_exist_is_rejected() {
        UUID seriesId = UUID.randomUUID();
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(seriesId, spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.RecurringSeriesNotFound.class);
    }

    @Test
    void resolves_custom_shares_that_sum_to_the_amount_before_calling_the_repository() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), aliceId,
            List.of(new ContributionInput(aliceId, new BigDecimal("600.00")), new ContributionInput(bobId, new BigDecimal("250.00"))),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        when(seriesRepository.findById(command.seriesId())).thenReturn(Optional.of(existingInSameSpace(command.seriesId())));
        List<Contribution> expectedResolved = List.of(new Contribution(aliceId, new BigDecimal("600.00")), new Contribution(bobId, new BigDecimal("250.00")));
        RecurringTransactionSeries updated = new RecurringTransactionSeries(command.seriesId(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, command.categoryId(), aliceId, expectedResolved,
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, null);
        when(seriesRepository.update(command, expectedResolved)).thenReturn(updated);

        RecurringTransactionSeries result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
        verify(seriesRepository).update(command, expectedResolved);
    }

    @Test
    void updating_a_recurring_series_with_custom_shares_that_do_not_sum_to_the_amount_is_rejected() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), aliceId,
            List.of(new ContributionInput(aliceId, new BigDecimal("500.00")), new ContributionInput(bobId, new BigDecimal("200.00"))),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.InvalidContributionShares.class);
        verify(seriesRepository, never()).update(any(), any());
    }

    @Test
    void updating_a_split_recurring_series_with_no_payer_is_rejected() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), null,
            List.of(new ContributionInput(aliceId, null), new ContributionInput(bobId, null)),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.PayerRequired.class);
        verify(seriesRepository, never()).update(any(), any());
    }

    @Test
    void updating_a_recurring_series_with_an_end_date_before_the_anchor_date_is_rejected() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Prêt voiture",
            new BigDecimal("250.00"), TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.InvalidEndDate.class);
        verify(seriesRepository, never()).update(any(), any());
    }

    @Test
    void updating_a_recurring_series_with_a_category_that_does_not_exist_is_rejected() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.CategoryNotFound.class);
        verify(seriesRepository, never()).update(any(), any());
    }

    @Test
    void updating_a_recurring_series_with_a_category_from_another_space_is_rejected() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        Category foreignCategory = new Category(command.categoryId(), UUID.randomUUID(), "Alimentation", "#f59e0b", "Utensils", true);
        when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(foreignCategory));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.CategoryNotFound.class);
        verify(seriesRepository, never()).update(any(), any());
    }

    @Test
    void validates_the_payer_and_every_contributor_belong_to_the_space() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), aliceId,
            List.of(new ContributionInput(aliceId, new BigDecimal("600.00")), new ContributionInput(bobId, new BigDecimal("250.00"))),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        when(seriesRepository.findById(command.seriesId())).thenReturn(Optional.of(existingInSameSpace(command.seriesId())));
        when(seriesRepository.update(any(), any())).thenReturn(existingInSameSpace(command.seriesId()));

        handler.update(command, membership(SpaceRole.MEMBER));

        verify(spaceMemberValidator, atLeastOnce()).ensureMember(spaceId, aliceId);
        verify(spaceMemberValidator).ensureMember(spaceId, bobId);
    }

    @Test
    void rejects_an_update_whose_payer_is_not_actually_a_member_of_the_space() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), aliceId, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);
        doThrow(new FinanceException.MemberNotInSpace()).when(spaceMemberValidator).ensureMember(spaceId, aliceId);

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.MemberNotInSpace.class);
        verify(seriesRepository, never()).update(any(), any());
    }

    @Test
    void a_viewer_cannot_update_a_recurring_series() {
        UpdateRecurringSeriesCommand command = new UpdateRecurringSeriesCommand(UUID.randomUUID(), spaceId, "Loyer modifié",
            new BigDecimal("850.00"), TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
