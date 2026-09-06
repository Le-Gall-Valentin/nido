package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRecurringSeriesHandlerTest {

    @Mock RecurringTransactionSeriesRepository seriesRepository;
    private ListRecurringSeriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListRecurringSeriesHandler(seriesRepository);
    }

    @Test
    void lists_every_series_in_the_callers_space() {
        RecurringTransactionSeries series = new RecurringTransactionSeries(UUID.randomUUID(), spaceId, "Loyer",
            new BigDecimal("800.00"), TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, null);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));
        SpaceMembership membership = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        List<RecurringTransactionSeries> result = handler.list(membership);

        assertThat(result).containsExactly(series);
    }
}
