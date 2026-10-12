package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceMonthTotalEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionJpaRepository;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class RunningTotalsRepositoryAdapterIT extends RunningTotalsITSupport {

    @Autowired FinanceTransactionJpaRepository transactionJpa;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void the_first_read_builds_the_totals_of_everything_recorded_before_them() {
        create("Courses", "50.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());
        create("Salaire", "2000.00", TransactionType.INCOME, LocalDate.of(2026, 2, 1), aliceId, List.of());
        create("Dîner", "40.00", TransactionType.EXPENSE, LocalDate.of(2026, 2, 10), aliceId, half(aliceId, bobId, "40.00"));
        settlements.create(new CreateSettlementCommand(spaceId, bobId, aliceId, new BigDecimal("5.00"), LocalDate.of(2026, 3, 2)));

        assertNets(totals.findNetByMonth(spaceId), Map.of(
            YearMonth.of(2026, 1), "-50.00", YearMonth.of(2026, 2), "1960.00", YearMonth.of(2026, 3), "0"));
        assertNets(totals.findNetByMember(spaceId), Map.of(aliceId, "15.00", bobId, "-15.00"));
        assertThat(jdbc.queryForObject("SELECT version FROM finance_totals_states WHERE space_id = ?", Integer.class, spaceId))
            .isEqualTo(MonthTotalsWriter.TOTALS_VERSION);
    }

    @Test
    void the_totals_are_sealed_to_their_rows() {
        create("Courses", "50.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());
        totals.findNetByMonth(spaceId);

        Map<String, Object> row = jdbc.queryForMap("SELECT id, net_encrypted FROM finance_month_totals WHERE space_id = ?", spaceId);
        assertThat((String) row.get("net_encrypted")).startsWith("v3:").doesNotContain("50.00");
        assertThat(sealers.forSpace(spaceId).open(FinanceMonthTotalEntity.NET, (UUID) row.get("id"), (String) row.get("net_encrypted")))
            .isEqualTo("-50.00");
    }

    @Test
    void only_the_shared_operations_move_the_members() {
        // The two conditions of BalanceCalculator's guard. Getting them wrong does not throw: it silently changes who
        // owes whom, which is why they are checked against real SQL.
        create("Partagé", "40.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), aliceId, half(aliceId, bobId, "40.00"));
        create("Solo sans contributeurs", "15.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), aliceId, List.of());
        create("Sans payeur", "25.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null,
            List.of(new Contribution(bobId, new BigDecimal("25.00"))));

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "-80.00"));
        assertNets(totals.findNetByMember(spaceId), Map.of(aliceId, "20.00", bobId, "-20.00"));
    }

    @Test
    void a_shared_income_moves_the_members_the_other_way() {
        create("Remboursement partagé", "300.00", TransactionType.INCOME, LocalDate.of(2026, 1, 15), aliceId,
            half(aliceId, bobId, "300.00"));

        assertNets(totals.findNetByMember(spaceId), Map.of(aliceId, "-150.00", bobId, "150.00"));
    }

    @Test
    void the_build_never_reads_a_label() {
        Transaction shared = create("Partagé", "40.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), aliceId,
            half(aliceId, bobId, "40.00"));
        FinanceTransactionEntity stored = transactionJpa.findById(shared.id()).orElseThrow();
        stored.setLabelEncrypted("deadbeefdeadbeefdeadbeefdeadbeef");
        transactionJpa.saveAndFlush(stored);

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "-40.00"));
        assertThatThrownBy(() -> transactions.findById(shared.id()))
            .as("a read of the whole operation does open the label — otherwise this test proves nothing")
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void an_amount_copied_from_another_row_stops_the_build() {
        Transaction rent = create("Loyer", "850.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 5), null, List.of());
        Transaction coffee = create("Café", "3.50", TransactionType.EXPENSE, LocalDate.of(2026, 1, 6), null, List.of());
        jdbc.update("UPDATE finance_transactions SET amount_encrypted = (SELECT amount_encrypted FROM finance_transactions WHERE id = ?) WHERE id = ?",
            rent.id(), coffee.id());

        assertThatThrownBy(() -> totals.findNetByMonth(spaceId)).isInstanceOf(SealedValueRejected.class);
    }

    @Test
    void refresh_folds_a_month_again_from_its_operations() {
        create("Courses", "50.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());
        totals.findNetByMonth(spaceId);
        jdbc.update("DELETE FROM finance_month_totals WHERE space_id = ?", spaceId);

        writer.refresh(spaceId, List.of(YearMonth.of(2026, 1)));

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "-50.00"));
    }

    @Test
    void a_raised_version_rebuilds_the_totals() {
        create("Courses", "50.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());
        totals.findNetByMonth(spaceId);
        jdbc.update("DELETE FROM finance_month_totals WHERE space_id = ?", spaceId);
        jdbc.update("UPDATE finance_totals_states SET version = 0 WHERE space_id = ?", spaceId);

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "-50.00"));
    }

    @Test
    void another_space_is_left_out() {
        UUID elsewhere = space("Ailleurs");
        Category otherCategory = categories.create(
            new CreateCategoryCommand(elsewhere, "Courses", "#f59e0b", "Utensils", TransactionType.EXPENSE), true);
        transactions.create(new CreateTransactionCommand(elsewhere, "Ailleurs", new BigDecimal("99.00"), TransactionType.EXPENSE,
                otherCategory.id(), LocalDate.of(2026, 1, 15), aliceId, List.of(new ContributionInput(bobId, new BigDecimal("99.00"))), null),
            List.of(new Contribution(bobId, new BigDecimal("99.00"))));

        assertThat(totals.findNetByMonth(spaceId)).isEmpty();
        assertThat(totals.findNetByMember(spaceId)).isEmpty();
    }

    @Test
    void two_first_reads_at_the_same_time_build_once() throws Exception {
        create("Courses", "50.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());
        create("Salaire", "2000.00", TransactionType.INCOME, LocalDate.of(2026, 2, 1), aliceId, List.of());
        jdbc.update("DELETE FROM finance_totals_states WHERE space_id = ?", spaceId);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Map<YearMonth, BigDecimal>>> reads = List.of(
                pool.submit(() -> { start.await(); return totals.findNetByMonth(spaceId); }),
                pool.submit(() -> { start.await(); return totals.findNetByMonth(spaceId); }));
            start.countDown();
            for (Future<Map<YearMonth, BigDecimal>> read : reads) {
                assertNets(read.get(), Map.of(YearMonth.of(2026, 1), "-50.00", YearMonth.of(2026, 2), "2000.00"));
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finance_month_totals WHERE space_id = ?", Integer.class, spaceId))
            .isEqualTo(2);
    }

    @Test
    void a_read_of_built_totals_takes_no_lock() {
        // Every finance read once queued behind its space's lock for work that did not exist (B8). Only the first read
        // of a space, the one that builds its totals, may take the totals lock.
        create("Courses", "50.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());
        totals.findNetByMonth(spaceId);

        Integer locks = new TransactionTemplate(transactionManager).execute(status -> {
            totals.findNetByMonth(spaceId);
            totals.findNetByMember(spaceId);
            return jdbc.queryForObject("select count(*) from pg_locks where locktype = 'advisory' and pid = pg_backend_pid()",
                Integer.class);
        });

        assertThat(locks).isZero();
    }
}
