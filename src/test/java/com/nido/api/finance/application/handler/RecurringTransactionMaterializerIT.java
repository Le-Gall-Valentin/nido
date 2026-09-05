package com.nido.api.finance.application.handler;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.finance.infrastructure.persistence.adapter.CategoryRepositoryAdapter;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for a real bug: {@code GetFinanceStatsHandler}, {@code ListTransactionsHandler}
 * and {@code GetProjectionHandler} each independently trigger lazy materialization on read, and
 * FinancePage fires all three in parallel on load — without serialization, concurrent callers could
 * all read the same {@code lastMaterializedDate} and each insert the same due occurrence.
 */
@IntegrationTestConfig
class RecurringTransactionMaterializerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired GetFinanceStatsHandler statsHandler;
    @Autowired TransactionRepository transactionRepository;
    @Autowired RecurringTransactionSeriesRepository seriesRepository;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired CategoryRepositoryAdapter categoryAdapter;

    private UUID spaceId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();

        Category category = categoryAdapter.create(new CreateCategoryCommand(spaceId, "Abonnements", "#f59e0b", "Repeat"), true);
        categoryId = category.id();
    }

    @Test
    void never_materializes_the_same_due_occurrence_twice_for_concurrent_readers() throws Exception {
        LocalDate anchor = LocalDate.of(2026, 1, 5);
        seriesRepository.create(new CreateRecurringSeriesCommand(
            spaceId, "Abonnement", new BigDecimal("19.99"), TransactionType.EXPENSE, categoryId, null,
            List.of(), RecurrenceInterval.MONTHLY, 1, anchor, null), List.of());
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        int concurrentReads = 5;
        CyclicBarrier barrier = new CyclicBarrier(concurrentReads);
        ExecutorService pool = Executors.newFixedThreadPool(concurrentReads);
        try {
            List<Future<?>> futures = IntStream.range(0, concurrentReads)
                .<Future<?>>mapToObj(i -> pool.submit(() -> getStatsAfterBarrier(barrier, caller, anchor)))
                .toList();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdown();
        }

        List<Transaction> materialized = transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1));
        assertThat(materialized).hasSize(1);
        assertThat(materialized.get(0).date()).isEqualTo(anchor);
    }

    private void getStatsAfterBarrier(CyclicBarrier barrier, SpaceMembership caller, LocalDate today) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        statsHandler.getStats(YearMonth.of(2026, 1), caller, today);
    }
}
