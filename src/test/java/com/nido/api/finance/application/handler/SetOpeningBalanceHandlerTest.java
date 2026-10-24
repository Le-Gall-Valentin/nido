package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.service.BalanceNotifier;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.model.SetOpeningBalanceCommand;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetOpeningBalanceHandlerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    @Mock OpeningBalanceRepository openingBalances;
    @Mock GetSpaceTodayUseCase spaceToday;
    @Mock BalanceNotifier notifier;
    private SetOpeningBalanceHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new SetOpeningBalanceHandler(openingBalances, spaceToday, notifier);
        lenient().when(spaceToday.today(spaceId)).thenReturn(TODAY);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void saves_the_opening_balance_of_today_or_before_overdraft_included() {
        handler.set(new SetOpeningBalanceCommand(spaceId, new BigDecimal("-150.50"), TODAY), membership(SpaceRole.MEMBER));

        verify(openingBalances).save(new OpeningBalance(spaceId, new BigDecimal("-150.50"), TODAY));
    }

    @Test
    void refuses_a_day_after_today_on_the_household_calendar() {
        assertThatThrownBy(() -> handler.set(new SetOpeningBalanceCommand(spaceId, BigDecimal.TEN, TODAY.plusDays(1)), membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.OpeningBalanceInFuture.class);
        verify(openingBalances, never()).save(any());
    }

    @Test
    void a_viewer_cannot_set_it() {
        assertThatThrownBy(() -> handler.set(new SetOpeningBalanceCommand(spaceId, BigDecimal.TEN, TODAY), membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void another_space_is_refused() {
        assertThatThrownBy(() -> handler.set(new SetOpeningBalanceCommand(UUID.randomUUID(), BigDecimal.TEN, TODAY), membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
    }

    @Test
    void the_notifier_is_told_what_the_opening_balance_was_and_what_it_is_after_writing() {
        OpeningBalance before = new OpeningBalance(spaceId, new BigDecimal("100.00"), TODAY.minusDays(3));
        OpeningBalance after = new OpeningBalance(spaceId, new BigDecimal("-150.50"), TODAY);
        when(openingBalances.findBySpaceId(spaceId)).thenReturn(Optional.of(before));
        when(openingBalances.save(after)).thenReturn(after);
        SpaceMembership caller = membership(SpaceRole.MEMBER);

        handler.set(new SetOpeningBalanceCommand(spaceId, new BigDecimal("-150.50"), TODAY), caller);

        InOrder order = inOrder(openingBalances, notifier);
        order.verify(openingBalances).findBySpaceId(spaceId);
        order.verify(openingBalances).save(after);
        order.verify(notifier).openingBalanceSet(caller, Optional.of(before), after);
    }

    @Test
    void a_refused_gesture_tells_nobody() {
        assertThatThrownBy(() -> handler.set(new SetOpeningBalanceCommand(spaceId, BigDecimal.TEN, TODAY.plusDays(1)), membership(SpaceRole.MEMBER)));
        verifyNoInteractions(notifier);
    }
}
