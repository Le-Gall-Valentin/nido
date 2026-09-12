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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
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
    @Autowired JdbcTemplate jdbcTemplate;

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

        Category category = categoryAdapter.create(new CreateCategoryCommand(spaceId, "Abonnements", "#f59e0b", "Repeat", TransactionType.EXPENSE), true);
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

    @Test
    void still_materializes_a_fixed_term_series_backlog_even_after_its_end_date_has_already_passed() {
        // A 3-month loan: anchored Jan 1, ends Mar 1 — but nobody opens the Finances page
        // until August, long after the series "ended". Every occurrence up to the end date
        // is still owed and must still be materialized, not silently dropped because the
        // series looks expired by the time anyone reads it.
        LocalDate anchor = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 1);
        LocalDate today = LocalDate.of(2026, 8, 20);
        seriesRepository.create(new CreateRecurringSeriesCommand(
            spaceId, "Prêt voiture", new BigDecimal("250.00"), TransactionType.EXPENSE, categoryId, null,
            List.of(), RecurrenceInterval.MONTHLY, 1, anchor, endDate), List.of());
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        statsHandler.getStats(YearMonth.of(2026, 1), caller, today);

        List<Transaction> materialized = transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1));
        assertThat(materialized).extracting(Transaction::date).containsExactly(anchor);
        List<Transaction> marchOccurrence = transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 3));
        assertThat(marchOccurrence).extracting(Transaction::date).containsExactly(endDate);
        List<Transaction> aprilOnward = transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 4));
        assertThat(aprilOnward).isEmpty();
    }

    private void getStatsAfterBarrier(CyclicBarrier barrier, SpaceMembership caller, LocalDate today) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        statsHandler.getStats(YearMonth.of(2026, 1), caller, today);
    }
    // ─── B8 : une lecture sans rien à matérialiser n'ouvre pas le verrou ───

    /**
     * Advisory locks taken with {@code pg_advisory_xact_lock} are held until the transaction
     * ends, so counting them from inside the same transaction shows exactly what the read took.
     */
    private int advisoryLocksHeldByThisTransaction() {
        Integer held = jdbcTemplate.queryForObject(
            "select count(*) from pg_locks where locktype = 'advisory' and pid = pg_backend_pid()",
            Integer.class);
        return held == null ? 0 : held;
    }

    @Test
    @Transactional
    void a_read_with_nothing_due_takes_no_advisory_lock() {
        // The point of B8. Every finance read used to take the space's advisory lock before it
        // knew whether it had anything to do, so three readers of the same space queued behind
        // each other for work that did not exist — and each opened a write-intent transaction
        // to do nothing.
        LocalDate anchor = LocalDate.of(2026, 3, 1);
        seriesRepository.create(new CreateRecurringSeriesCommand(
            spaceId, "Abonnement", new BigDecimal("19.99"), TransactionType.EXPENSE, categoryId, null,
            List.of(), RecurrenceInterval.MONTHLY, 1, anchor, null), List.of());
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        // A month before the anchor: the series exists but owes nothing yet.
        statsHandler.getStats(YearMonth.of(2026, 2), caller, LocalDate.of(2026, 2, 10));

        assertThat(advisoryLocksHeldByThisTransaction())
            .as("a read that has nothing to materialize must not serialize every other reader "
                + "of the same space behind an advisory lock")
            .isZero();
        assertThat(transactionRepository.findAllBySpaceId(spaceId)).isEmpty();
    }

    @Test
    @Transactional
    void a_read_with_something_due_does_take_the_advisory_lock() {
        // The other half: the pre-check must not have quietly disabled serialization for the
        // case it exists to protect.
        LocalDate anchor = LocalDate.of(2026, 1, 5);
        seriesRepository.create(new CreateRecurringSeriesCommand(
            spaceId, "Abonnement", new BigDecimal("19.99"), TransactionType.EXPENSE, categoryId, null,
            List.of(), RecurrenceInterval.MONTHLY, 1, anchor, null), List.of());
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        statsHandler.getStats(YearMonth.of(2026, 1), caller, LocalDate.of(2026, 1, 10));

        assertThat(advisoryLocksHeldByThisTransaction()).isEqualTo(1);
        assertThat(transactionRepository.findAllBySpaceId(spaceId))
            .extracting(Transaction::date).containsExactly(anchor);
    }
}
