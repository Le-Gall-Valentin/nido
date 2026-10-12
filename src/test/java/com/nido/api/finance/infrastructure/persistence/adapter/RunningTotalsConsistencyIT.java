package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.finance.domain.model.BalanceCalculator;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.model.SplitTransaction;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nido.api.finance.FinanceTestReads.allTransactions;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class RunningTotalsConsistencyIT extends RunningTotalsITSupport {

    /**
     * Writes at random — creations, edits, deletions, settlements — and checks after every one that the totals are
     * still exactly the fold of the whole ledger. A fixed seed makes a failure replayable; the step is in the message.
     */
    @ParameterizedTest
    @ValueSource(longs = {7, 2026})
    void the_totals_stay_the_fold_of_the_ledger_whatever_the_writes(long seed) {
        Random random = new Random(seed);
        List<UUID> live = new ArrayList<>();
        for (int step = 0; step < 120; step++) {
            int pick = random.nextInt(10);
            if (live.isEmpty() || pick < 5) {
                live.add(write(random, null).id());
            } else if (pick < 8) {
                write(random, live.get(random.nextInt(live.size())));
            } else if (pick < 9) {
                transactions.delete(live.remove(random.nextInt(live.size())));
            } else {
                settlements.create(new CreateSettlementCommand(spaceId, bobId, aliceId, amount(random), date(random)));
            }
            assertTotalsAreTheFoldOfTheLedger("seed " + seed + ", step " + step);
        }
    }

    /** Creates an operation, or rewrites every field of an existing one. */
    private Transaction write(Random random, UUID existing) {
        BigDecimal amount = amount(random);
        TransactionType type = random.nextInt(5) == 0 ? TransactionType.INCOME : TransactionType.EXPENSE;
        boolean shared = random.nextBoolean();
        UUID payer = shared || random.nextBoolean() ? (random.nextBoolean() ? aliceId : bobId) : null;
        List<Contribution> shares = shared ? split(amount) : List.of();
        List<ContributionInput> inputs = shares.stream().map(c -> new ContributionInput(c.memberId(), c.shareAmount())).toList();
        LocalDate date = date(random);
        if (existing == null) {
            return transactions.create(new CreateTransactionCommand(spaceId, "Op", amount, type, categoryId, date, payer, inputs, null), shares);
        }
        return transactions.update(new UpdateTransactionCommand(existing, spaceId, "Op", amount, type, categoryId, date, payer, inputs), shares);
    }

    private void assertTotalsAreTheFoldOfTheLedger(String where) {
        List<Transaction> ledger = allTransactions(transactions, spaceId);
        List<SettlementRecord> settled = settlements.findBetweenMembers(spaceId, aliceId, bobId);
        Map<YearMonth, BigDecimal> expectedNets = ledger.stream().collect(Collectors.groupingBy(t -> YearMonth.from(t.date()),
            Collectors.reducing(BigDecimal.ZERO, t -> t.type() == TransactionType.INCOME ? t.amount() : t.amount().negate(), BigDecimal::add)));
        Map<UUID, BigDecimal> expectedMembers = BalanceCalculator.fold(
            ledger.stream().map(t -> new SplitTransaction(t.payerId(), t.amount(), t.contributors(), t.type())).toList(), settled);

        assertThat(nonZero(totals.findNetByMonth(spaceId))).as(where).isEqualTo(nonZero(expectedNets));
        assertThat(scaled(totals.findNetByMember(spaceId))).as(where).isEqualTo(scaled(expectedMembers));
    }

    /** A month whose operations cancel out, or that holds only a settlement, nets zero: kept or not, it adds nothing. */
    private static Map<YearMonth, BigDecimal> nonZero(Map<YearMonth, BigDecimal> nets) {
        return nets.entrySet().stream().filter(e -> e.getValue().signum() != 0)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().setScale(2)));
    }

    private static Map<UUID, BigDecimal> scaled(Map<UUID, BigDecimal> nets) {
        return nets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().setScale(2)));
    }

    private List<Contribution> split(BigDecimal amount) {
        BigDecimal half = amount.divide(BigDecimal.valueOf(2), 2, RoundingMode.DOWN);
        return List.of(new Contribution(aliceId, half), new Contribution(bobId, amount.subtract(half)));
    }

    private static BigDecimal amount(Random random) {
        return BigDecimal.valueOf(100 + random.nextInt(49_900), 2);
    }

    private static LocalDate date(Random random) {
        return LocalDate.of(2025 + random.nextInt(2), 1 + random.nextInt(12), 1 + random.nextInt(28));
    }
}
