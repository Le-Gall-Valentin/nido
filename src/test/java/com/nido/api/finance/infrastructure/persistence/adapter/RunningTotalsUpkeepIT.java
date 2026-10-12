package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class RunningTotalsUpkeepIT extends RunningTotalsITSupport {

    @Autowired PlatformTransactionManager transactionManager;
    @Autowired FinanceTransactionJpaRepository transactionJpa;

    @BeforeEach
    void buildFirst() {
        // Built before each write, so what follows checks the upkeep — not a build that would mend everything.
        totals.findNetByMonth(spaceId);
    }

    @Test
    void a_new_operation_counts_in_its_month() {
        create("Courses", "30.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "-30.00"));
    }

    @Test
    void an_edit_of_the_amount_or_the_type_counts_once() {
        Transaction groceries = create("Courses", "30.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());

        transactions.update(new UpdateTransactionCommand(groceries.id(), spaceId, "Remboursement", new BigDecimal("45.00"),
            TransactionType.INCOME, categoryId, LocalDate.of(2026, 1, 15), null, List.of()), List.of());

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "45.00"));
    }

    @Test
    void an_operation_moved_to_another_month_leaves_the_first_one() {
        Transaction groceries = create("Courses", "30.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());
        create("Pain", "10.00", TransactionType.EXPENSE, LocalDate.of(2026, 3, 1), null, List.of());

        transactions.update(new UpdateTransactionCommand(groceries.id(), spaceId, "Courses", new BigDecimal("30.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 3, 3), null, List.of()), List.of());

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 3), "-40.00"));
    }

    @Test
    void an_edit_of_the_payer_or_the_shares_moves_the_members() {
        Transaction dinner = create("Dîner", "40.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), aliceId,
            half(aliceId, bobId, "40.00"));

        List<Contribution> shares = half(aliceId, bobId, "40.00");
        transactions.update(new UpdateTransactionCommand(dinner.id(), spaceId, "Dîner", new BigDecimal("40.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 15), bobId,
            shares.stream().map(c -> new ContributionInput(c.memberId(), c.shareAmount())).toList()), shares);

        assertNets(totals.findNetByMember(spaceId), Map.of(aliceId, "-20.00", bobId, "20.00"));
    }

    @Test
    void a_deleted_operation_leaves_its_month() {
        Transaction groceries = create("Courses", "30.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());
        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "-30.00"));

        transactions.delete(groceries.id());

        assertThat(totals.findNetByMonth(spaceId)).isEmpty();
    }

    @Test
    void a_settlement_moves_the_members_of_its_month_and_both_stay_listed_at_zero() {
        create("Dîner", "40.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), aliceId, half(aliceId, bobId, "40.00"));

        settlements.create(new CreateSettlementCommand(spaceId, bobId, aliceId, new BigDecimal("20.00"), LocalDate.of(2026, 2, 1)));

        assertNets(totals.findNetByMember(spaceId), Map.of(aliceId, "0", bobId, "0"));
    }

    @Test
    void an_edit_refreshes_the_space_of_the_row_whatever_the_command_says() {
        Transaction groceries = create("Courses", "30.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());

        transactions.update(new UpdateTransactionCommand(groceries.id(), space("Autre"), "Courses", new BigDecimal("35.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 15), null, List.of()), List.of());

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "-35.00"));
    }

    @Test
    void two_operations_saved_at_once_in_the_same_month_both_count() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Transaction>> writes = List.of(
                pool.submit(() -> { start.await(); return create("Un", "10.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 3), null, List.of()); }),
                pool.submit(() -> { start.await(); return create("Deux", "20.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 4), null, List.of()); }));
            start.countDown();
            for (Future<Transaction> write : writes) {
                write.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 1), "-30.00"));
    }

    @Test
    void sumRecordedBetween_adds_the_operations_of_the_range_with_their_sign() {
        create("Courses", "30.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 5), null, List.of());
        create("Salaire", "100.00", TransactionType.INCOME, LocalDate.of(2026, 1, 10), aliceId, List.of());
        create("Pain", "5.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 20), null, List.of());

        assertThat(transactions.sumRecordedBetween(spaceId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10)))
            .isEqualByComparingTo("70.00");
    }

    @Test
    void an_edit_that_read_the_operation_before_another_moved_it_does_not_leave_it_counted_twice() {
        Transaction groceries = create("Courses", "30.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            // Loaded in January, the way UpdateTransactionHandler loads it before calling the adapter…
            transactionJpa.findById(groceries.id()).orElseThrow();
            // …while another request moves it to February and commits.
            inAnotherTransaction(() -> transactions.update(moved(groceries, LocalDate.of(2026, 2, 10)), List.of()));
            transactions.update(moved(groceries, LocalDate.of(2026, 3, 5)), List.of());
        });

        assertNets(totals.findNetByMonth(spaceId), Map.of(YearMonth.of(2026, 3), "-30.00"));
    }

    @Test
    void a_deletion_that_read_the_operation_before_another_moved_it_leaves_no_month_counting_it() {
        Transaction groceries = create("Courses", "30.00", TransactionType.EXPENSE, LocalDate.of(2026, 1, 15), null, List.of());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            transactionJpa.findById(groceries.id()).orElseThrow();
            inAnotherTransaction(() -> transactions.update(moved(groceries, LocalDate.of(2026, 2, 10)), List.of()));
            transactions.delete(groceries.id());
        });

        assertThat(totals.findNetByMonth(spaceId)).isEmpty();
    }

    private UpdateTransactionCommand moved(Transaction transaction, LocalDate date) {
        return new UpdateTransactionCommand(transaction.id(), spaceId, transaction.label(), transaction.amount(), transaction.type(),
            categoryId, date, null, List.of());
    }

    /** Runs a write in a transaction of its own, on another thread, and waits for it to commit. */
    private static void inAnotherTransaction(Runnable write) {
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            other.submit(write).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        } finally {
            other.shutdownNow();
        }
    }
}
