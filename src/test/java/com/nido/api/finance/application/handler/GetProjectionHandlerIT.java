package com.nido.api.finance.application.handler;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.Projection;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.infrastructure.persistence.adapter.CategoryRepositoryAdapter;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The projection read end to end, on a chosen day: the occurrences it creates, then the totals it reads them from. */
@IntegrationTestConfig
class GetProjectionHandlerIT {

    @Autowired GetProjectionHandler projectionHandler;
    @Autowired RecurringTransactionSeriesRepository seriesRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired CategoryRepositoryAdapter categoryAdapter;

    private UUID spaceId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        spaceJpaRepository.deleteAll();
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, "Chez nous");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();
        categoryId = categoryAdapter.create(new CreateCategoryCommand(spaceId, "Logement", "#f59e0b", "House", TransactionType.EXPENSE), true).id();
    }

    @AfterEach
    void cleanUp() {
        spaceJpaRepository.deleteAll();
    }

    @Test
    void on_the_first_of_the_month_the_rent_due_that_day_is_created_by_the_read_and_counted_once() {
        seriesRepository.create(new CreateRecurringSeriesCommand(spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE,
            categoryId, null, List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null), List.of());
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        Projection projection = projectionHandler.getProjection(YearMonth.of(2026, 1), caller, LocalDate.of(2026, 1, 1));

        assertThat(projection.recordedEndOfMonthBalance()).as("created by this read, then read from the totals").isEqualByComparingTo("-800.00");
        assertThat(projection.upcoming()).isEmpty();
        assertThat(projection.projectedEndOfMonthBalance()).isEqualByComparingTo("-800.00");
    }
}
